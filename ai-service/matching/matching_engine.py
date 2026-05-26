import os
import json
import re
from sentence_transformers import SentenceTransformer, util
from openai import OpenAI
from parser.resume_parser import parse_skills

# Lazy loading of local sentence-transformers model
model = None

def get_sentence_transformer_model():
    global model
    if model is None:
        try:
            model = SentenceTransformer('all-MiniLM-L6-v2')
        except Exception as e:
            print(f"Error loading SentenceTransformer: {e}")
    return model

def calculate_local_match(resume_text: str, candidate_skills: list[str], job_description: str, required_skills: list[str]) -> dict:
    """Calculates matching score locally using SentenceTransformers and skill overlap."""
    # 1. Skill Overlap Calculation
    if not required_skills:
        skill_score = 100.0
        matched_skills = candidate_skills
        missing_skills = []
    else:
        req_skills_lower = [s.lower() for s in required_skills]
        cand_skills_lower = [s.lower() for s in candidate_skills]
        
        matched_skills = []
        missing_skills = []
        
        # Check matching
        for req_skill in required_skills:
            # Check for direct or partial match
            # E.g. "React" matches "React.js" or vice versa
            matched_flag = False
            for cand_skill in candidate_skills:
                if req_skill.lower() in cand_skill.lower() or cand_skill.lower() in req_skill.lower():
                    matched_skills.append(cand_skill)
                    matched_flag = True
                    break
            if not matched_flag:
                missing_skills.append(req_skill)
                
        # Unique and clean matched list
        matched_skills = list(set(matched_skills))
        
        # Calculate raw overlap ratio
        overlap_ratio = len(matched_skills) / len(required_skills)
        skill_score = overlap_ratio * 100.0
        
    # 2. Semantic Similarity Calculation
    semantic_score = 50.0 # Default if model fails
    t_model = get_sentence_transformer_model()
    if t_model:
        try:
            # Truncate text if it is extremely long to prevent token limitations or slowdowns
            res_emb = t_model.encode(resume_text[:8000], convert_to_tensor=True)
            job_emb = t_model.encode(job_description[:8000], convert_to_tensor=True)
            similarity = util.cos_sim(res_emb, job_emb)
            
            # Map cosine similarity (usually 0.2 to 0.8 for related texts) to a 0-100 range
            raw_sim = float(similarity[0][0])
            # Scale range [0.1, 0.7] to [0, 100]
            mapped_sim = ((raw_sim - 0.1) / 0.6) * 100.0
            semantic_score = max(0.0, min(100.0, mapped_sim))
        except Exception as e:
            print(f"Error encoding embeddings: {e}")
            
    # 3. Final Weighted Score
    # 60% skill match, 40% semantic content match
    final_score = (0.6 * skill_score) + (0.4 * semantic_score)
    final_score = round(max(0.0, min(100.0, final_score)), 1)
    
    # 4. Generate AI Summary locally
    matched_skills_str = ", ".join(matched_skills) if matched_skills else "None"
    missing_skills_str = ", ".join(missing_skills) if missing_skills else "None"
    
    # Simple templates based on scores
    if final_score >= 80:
        fit_level = "Excellent"
        critique = "The candidate is a strong fit with a high alignment in core skills and background."
    elif final_score >= 60:
        fit_level = "Good"
        critique = "The candidate is a solid match, possessing most of the required skills but lacking a few technical areas."
    elif final_score >= 40:
        fit_level = "Average"
        critique = "The candidate meets some requirements but has notable skill gaps that may require training."
    else:
        fit_level = "Weak"
        critique = "The candidate has very low alignment with the job description and key technical criteria."
        
    ai_summary = (
        f"Overall Match: {fit_level} ({final_score}% score).\n\n"
        f"Strengths: Strong match on key skills: {matched_skills_str}.\n"
        f"Gaps: Missing critical skillsets: {missing_skills_str}.\n\n"
        f"Recommendation: {critique} Semantic analysis suggests a similarity score of {round(semantic_score, 1)}%."
    )
    
    return {
        "match_score": final_score,
        "matched_skills": matched_skills,
        "missing_skills": missing_skills,
        "ai_summary": ai_summary
    }

def calculate_openai_match(resume_text: str, candidate_skills: list[str], job_description: str, required_skills: list[str], api_key: str) -> dict:
    """Invokes OpenAI to calculate match score and generate a rich professional candidate report."""
    try:
        client = OpenAI(api_key=api_key)
        
        prompt = f"""
        You are an expert HR Technical Recruiter and AI Screener. Analyze the candidate's resume text against the Job Description.
        
        JOB DESCRIPTION:
        {job_description}
        
        REQUIRED SKILLS:
        {", ".join(required_skills)}
        
        CANDIDATE PARSED SKILLS:
        {", ".join(candidate_skills)}
        
        CANDIDATE RESUME TEXT (TRUNCATED):
        {resume_text[:6000]}
        
        Task:
        Evaluate the candidate and return a JSON object with:
        1. "match_score": A float between 0.0 and 100.0 indicating overall suitability.
        2. "matched_skills": A list of skills from REQUIRED SKILLS that the candidate possesses.
        3. "missing_skills": A list of skills from REQUIRED SKILLS that the candidate lacks.
        4. "ai_summary": A detailed professional assessment (3-4 sentences) outlining the candidate's strengths, experience gaps, and overall suitability.
        
        Return ONLY valid JSON. Do not include markdown code block formatting or wrap in ```json. Just return raw JSON.
        """
        
        response = client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[
                {"role": "system", "content": "You are a professional HR Screening Agent. You only output raw, valid JSON."},
                {"role": "user", "content": prompt}
            ],
            temperature=0.2,
            response_format={"type": "json_object"}
        )
        
        result_text = response.choices[0].message.content
        result_json = json.loads(result_text)
        
        # Validate keys
        return {
            "match_score": float(result_json.get("match_score", 0)),
            "matched_skills": list(result_json.get("matched_skills", [])),
            "missing_skills": list(result_json.get("missing_skills", [])),
            "ai_summary": str(result_json.get("ai_summary", ""))
        }
    except Exception as e:
        print(f"OpenAI Match execution failed: {e}. Falling back to local match...")
        return calculate_local_match(resume_text, candidate_skills, job_description, required_skills)

def match_resume_to_job(resume_text: str, candidate_skills: list[str], job_description: str, required_skills: list[str]) -> dict:
    """Determines whether to use local similarity or OpenAI matching depending on environmental configurations."""
    # Prioritize candidate skills if already parsed, or parse on the fly
    if not candidate_skills:
        candidate_skills = parse_skills(resume_text)
        
    api_key = os.getenv("OPENAI_API_KEY")
    if api_key:
        return calculate_openai_match(resume_text, candidate_skills, job_description, required_skills, api_key)
    else:
        return calculate_local_match(resume_text, candidate_skills, job_description, required_skills)
