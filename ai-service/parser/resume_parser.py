import re
import fitz  # PyMuPDF
import spacy

# Self-installing spacy model configuration
nlp = None
try:
    nlp = spacy.load("en_core_web_sm")
except OSError:
    try:
        from spacy.cli import download
        download("en_core_web_sm")
        nlp = spacy.load("en_core_web_sm")
    except Exception:
        # Fallback to None if spaCy is completely unavailable or internet is offline
        pass

# Expanded list of common technology skills
SKILLS_DB = [
    "python", "java", "javascript", "typescript", "c++", "c#", "ruby", "go", "golang", "rust", "php", "swift", "kotlin", "objective-c",
    "html", "css", "html5", "css3", "sass", "scss", "tailwind", "bootstrap",
    "react", "react.js", "next.js", "nextjs", "vue", "vue.js", "angular", "svelte", "jquery",
    "node.js", "nodejs", "express", "express.js", "django", "flask", "fastapi", "nest.js", "nestjs",
    "spring", "spring boot", "spring-boot", "hibernate", "jpa", "microservices",
    "sql", "mysql", "postgresql", "postgres", "mongodb", "redis", "cassandra", "dynamodb", "sqlite", "oracle",
    "aws", "amazon web services", "azure", "gcp", "google cloud", "google cloud platform",
    "docker", "kubernetes", "k8s", "terraform", "ansible", "jenkins", "github actions", "gitlab ci", "ci/cd", "circleci",
    "git", "github", "gitlab", "bitbucket",
    "machine learning", "deep learning", "nlp", "natural language processing", "computer vision", "tensorflow", "pytorch", "scikit-learn", "keras", "pandas", "numpy",
    "rest api", "graphql", "grpc", "soap",
    "linux", "unix", "ubuntu", "nginx", "apache",
    "agile", "scrum", "jira", "confluence",
    "testing", "jest", "cypress", "selenium", "junit", "pytest", "mocha",
    "data structures", "algorithms", "system design", "oop", "object-oriented programming"
]

def extract_text_from_pdf(pdf_bytes: bytes) -> str:
    """Extracts all text from a PDF byte stream using PyMuPDF."""
    text = ""
    try:
        doc = fitz.open(stream=pdf_bytes, filetype="pdf")
        for page in doc:
            text += page.get_text() + "\n"
        doc.close()
    except Exception as e:
        print(f"Error reading PDF: {e}")
    return text

def parse_name(text: str) -> str:
    """Attempts to extract the candidate name from the first few lines of text."""
    lines = [line.strip() for line in text.split("\n") if line.strip()]
    if not lines:
        return "Unknown Candidate"
    
    # Try spaCy NER on the first 3 lines
    header_text = "\n".join(lines[:3])
    if nlp:
        doc = nlp(header_text)
        for ent in doc.ents:
            if ent.label_ == "PERSON" and len(ent.text.split()) >= 2:
                # Name should typically be 2 or more words
                # Filter out standard non-name words
                name = ent.text.strip().replace("\n", " ")
                if not any(word in name.lower() for word in ["resume", "curriculum", "vitae", "pdf", "page", "phone", "email"]):
                    return name

    # Fallback: find the first line that doesn't contain contact info or standard resume keywords
    for line in lines[:3]:
        # Skip lines with @ (email), digits (phone/address), or common words
        if "@" not in line and not re.search(r'\d', line):
            if len(line.split()) >= 2 and len(line) < 40:
                # Make sure it's 2-4 words and reasonably sized
                return line
                
    return lines[0] if len(lines[0]) < 40 else "Unknown Candidate"

def parse_email(text: str) -> str:
    """Extracts email address using standard regex."""
    email_regex = r'[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+'
    match = re.search(email_regex, text)
    return match.group(0) if match else ""

def parse_phone(text: str) -> str:
    """Extracts phone number using standard regex."""
    # Matches common phone patterns: e.g. +1 555-555-5555, (555) 555-5555, 555.555.5555, etc.
    phone_regex = r'(?:\+?\d{1,3}[-.\s]?)?\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4}'
    match = re.search(phone_regex, text)
    return match.group(0) if match else ""

def parse_skills(text: str) -> list[str]:
    """Matches text against a list of common technical skills."""
    text_lower = text.lower()
    found_skills = []
    
    for skill in SKILLS_DB:
        # Avoid partial matches, e.g. "go" matching inside "good" or "c" inside "cat"
        # We look for word boundaries around the skill name
        # Handle special skills like c++, .net, c#
        escaped_skill = re.escape(skill)
        if skill in ["c++", "c#", "node.js", "next.js", "vue.js", "nest.js", "express.js", "react.js"]:
            # For these, boundary characters are handled specially
            pattern = rf'(?:^|[\s,;./()\-#])({escaped_skill})(?:$|[\s,;./()\-#])'
        else:
            pattern = rf'\b{escaped_skill}\b'
            
        if re.search(pattern, text_lower):
            # Normalize display name
            display_name = skill
            if skill == "golang":
                display_name = "Go"
            elif skill == "nodejs":
                display_name = "Node.js"
            elif skill == "nextjs":
                display_name = "Next.js"
            elif skill == "nestjs":
                display_name = "Nest.js"
            elif skill == "postgres":
                display_name = "PostgreSQL"
            else:
                # Capitalize acronyms or standard styling
                parts = skill.split()
                display_name = " ".join([p.capitalize() if p not in ["js", "api", "db", "sql", "aws", "gcp", "cv", "nlp", "oop"] else p.upper() for p in parts])
                # Special casing capitalization
                caps_mapping = {
                    "Python": "Python", "Java": "Java", "Javascript": "JavaScript", "Typescript": "TypeScript", 
                    "C++": "C++", "C#": "C#", "Html": "HTML", "Css": "CSS", "Html5": "HTML5", "Css3": "CSS3",
                    "Aws": "AWS", "Gcp": "GCP", "Sql": "SQL", "Mysql": "MySQL", "Postgresql": "PostgreSQL",
                    "Mongodb": "MongoDB", "Rest Api": "REST API", "Graphql": "GraphQL", "Grpc": "gRPC", "Soap": "SOAP",
                    "Ci/cd": "CI/CD", "Next.js": "Next.js", "Node.js": "Node.js", "Vue.js": "Vue.js", "Spring Boot": "Spring Boot"
                }
                if display_name in caps_mapping:
                    display_name = caps_mapping[display_name]
                    
            found_skills.append(display_name)
            
    return sorted(list(set(found_skills)))

def parse_experience_years(text: str) -> int:
    """Estimates the candidate's years of experience based on text keywords and date ranges."""
    text_lower = text.lower()
    
    # Heuristic 1: Look for phrases like "5 years of experience" or "8+ yrs experience"
    exp_regexes = [
        r'(\d+)\+?\s*(?:years?|yrs?)\s*(?:of)?\s*(?:exp|experience)',
        r'(?:exp|experience)\s*(?:of)?\s*(\d+)\+?\s*(?:years?|yrs?)',
        r'work\s+experience\s*:\s*(\d+)\+?\s*(?:years?|yrs?)'
    ]
    
    for r in exp_regexes:
        matches = re.findall(r, text_lower)
        if matches:
            years = [int(m) for m in matches if m.isdigit()]
            if years:
                return max(years)
                
    # Heuristic 2: Find all date ranges (years between 1990 and current year)
    # E.g. "2015 - 2018", "2018 to Present", "2019-2022"
    from datetime import datetime
    current_year = datetime.now().year
    
    date_range_regex = r'\b(199\d|20[0-2]\d)\s*(?:-|to|–)\s*(199\d|20[0-2]\d|present|current|now)\b'
    matches = re.findall(date_range_regex, text_lower)
    
    total_years = 0
    ranges = []
    
    for start, end in matches:
        start_year = int(start)
        if end in ["present", "current", "now"]:
            end_year = current_year
        else:
            end_year = int(end)
            
        if end_year >= start_year:
            duration = end_year - start_year
            # Cap realistic single-job durations
            if duration <= 40:
                ranges.append((start_year, end_year))
                
    # Merge overlapping intervals to get total unique years of experience
    if ranges:
        ranges.sort(key=lambda x: x[0])
        merged = []
        for r in ranges:
            if not merged or merged[-1][1] < r[0]:
                merged.append(r)
            else:
                merged[-1] = (merged[-1][0], max(merged[-1][1], r[1]))
                
        total_years = sum(end - start for start, end in merged)
        if total_years > 0:
            return min(total_years, 40) # cap at 40 years
            
    # Default fallback
    return 0

def parse_education(text: str) -> list[str]:
    """Extracts education milestones (degrees, institutions) from the text."""
    lines = text.split("\n")
    education_keywords = [
        "bachelor", "master", "doctor", "degree", "university", "college", "school", 
        "b.s", "m.s", "ph.d", "b.tech", "m.tech", "btech", "mtech", "b.c.a", "m.c.a", 
        "bca", "mca", "b.sc", "m.sc", "bsc", "msc", "mba", "diploma"
    ]
    
    education_lines = []
    in_edu_section = False
    
    for line in lines:
        line_strip = line.strip()
        if not line_strip:
            continue
            
        # Detect section transition
        if any(keyword in line_strip.lower() for keyword in ["education", "academic", "studies", "qualification"]):
            in_edu_section = True
            continue
        elif any(keyword in line_strip.lower() for keyword in ["experience", "work history", "employment", "skills", "projects"]):
            in_edu_section = False
            
        if in_edu_section:
            # If in the education section, keep lines that look relevant
            if len(line_strip) > 5 and len(line_strip) < 150:
                education_lines.append(line_strip)
        else:
            # If not in the section, only extract lines containing explicit degrees
            if any(re.search(rf'\b{re.escape(deg)}\b', line_strip.lower()) for deg in ["bachelor", "master", "ph.d", "b.tech", "m.tech", "btech", "mtech", "mba", "b.sc", "m.sc"]):
                if len(line_strip) > 5 and len(line_strip) < 150:
                    education_lines.append(line_strip)
                    
    # Clean duplicates and limit results
    seen = set()
    cleaned = []
    for line in education_lines:
        if line.lower() not in seen:
            seen.add(line.lower())
            cleaned.append(line)
            
    return cleaned[:4]  # Return top 4 entries

def parse_projects(text: str) -> list[str]:
    """Extracts key project details/titles from the resume."""
    lines = text.split("\n")
    project_entries = []
    in_project_section = False
    current_project = []
    
    for line in lines:
        line_strip = line.strip()
        if not line_strip:
            continue
            
        # Detect section transition
        if any(keyword in line_strip.lower() for keyword in ["projects", "personal projects", "academic projects", "key projects"]):
            in_project_section = True
            continue
        elif any(keyword in line_strip.lower() for keyword in ["experience", "work history", "education", "skills", "certifications"]):
            if in_project_section:
                # Save previous project before exiting
                if current_project:
                    project_entries.append(" ".join(current_project))
                    current_project = []
                in_project_section = False
                
        if in_project_section:
            # Collect lines
            # Projects often start with a bullet or strong heading
            if line_strip.startswith(("-", "*", "•", "▪")) or re.match(r'^[A-Z][a-zA-Z\s0-9]+:', line_strip):
                if current_project:
                    project_entries.append(" ".join(current_project))
                current_project = [line_strip.lstrip("-*•▪ ").strip()]
            else:
                if current_project:
                    current_project.append(line_strip)
                else:
                    current_project = [line_strip]
                    
    if current_project:
        project_entries.append(" ".join(current_project))
        
    # Clean and limit
    cleaned = []
    for proj in project_entries:
        p = proj.strip()
        if len(p) > 10 and len(p) < 300:
            cleaned.append(p)
            
    # Fallback if no explicit section
    if not cleaned:
        # Search for lines starting with project keywords or bold looking titles
        for line in lines:
            line_strip = line.strip()
            if any(keyword in line_strip.lower() for keyword in ["project:", "developed", "built", "implemented"]):
                if 10 < len(line_strip) < 150:
                    cleaned.append(line_strip.lstrip("-*•▪ ").strip())
                    if len(cleaned) >= 3:
                        break
                        
    return cleaned[:5]  # Limit to 5 projects

def parse_resume(pdf_bytes: bytes) -> dict:
    """Fully parses a PDF resume into its structural components."""
    parsed_text = extract_text_from_pdf(pdf_bytes)
    
    if not parsed_text.strip():
        return {
            "name": "Unknown",
            "email": "",
            "phone": "",
            "skills": [],
            "experience_years": 0,
            "education": [],
            "projects": [],
            "parsed_text": ""
        }
        
    return {
        "name": parse_name(parsed_text),
        "email": parse_email(parsed_text),
        "phone": parse_phone(parsed_text),
        "skills": parse_skills(parsed_text),
        "experience_years": parse_experience_years(parsed_text),
        "education": parse_education(parsed_text),
        "projects": parse_projects(parsed_text),
        "parsed_text": parsed_text
    }
