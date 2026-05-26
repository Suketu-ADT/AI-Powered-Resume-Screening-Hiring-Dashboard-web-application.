import uvicorn
from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional
import os

from parser.resume_parser import parse_resume
from matching.matching_engine import match_resume_to_job

app = FastAPI(
    title="AI Resume Screening Microservice",
    description="Microservice for parsing resumes and matching them to job descriptions using NLP and semantic embeddings.",
    version="1.0.0"
)

# Enable CORS for convenience, although Spring Boot acts as the main gateway
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Define matching request schema
class MatchRequest(BaseModel):
    resume_text: str
    job_description: str
    required_skills: List[str]
    candidate_skills: Optional[List[str]] = None

@app.get("/")
def read_root():
    return {
        "status": "online",
        "service": "AI Resume Screening Service",
        "openai_configured": os.getenv("OPENAI_API_KEY") is not None
    }

@app.post("/ai/parse")
async def parse_resume_endpoint(file: UploadFile = File(...)):
    """Receives a PDF resume, extracts the text, and parses structured fields."""
    if not file.filename.lower().endswith('.pdf'):
        raise HTTPException(status_code=400, detail="Only PDF resumes are supported.")
        
    try:
        file_bytes = await file.read()
        parsed_result = parse_resume(file_bytes)
        return parsed_result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to parse resume: {str(e)}")

@app.post("/ai/match")
async def match_resume_endpoint(payload: MatchRequest):
    """Calculates compatibility score and generates evaluation summary between a resume and job description."""
    try:
        match_result = match_resume_to_job(
            resume_text=payload.resume_text,
            candidate_skills=payload.candidate_skills or [],
            job_description=payload.job_description,
            required_skills=payload.required_skills
        )
        return match_result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to match candidate: {str(e)}")

if __name__ == "__main__":
    port = int(os.getenv("PORT", 8000))
    uvicorn.run("main:app", host="0.0.0.0", port=port, reload=True)
