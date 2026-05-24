# Karmic AI 

Karmic AI is a burnout prevention platform built specifically for India's medical workforce. It is anonymous by design, and built around the reality of a doctor's life.

## Try It Live
🌐 **[Open Karmic AI](https://karmic-ai-lake.vercel.app)**


## The Problem

Physician burnout in India is at a crisis level where residents working 36-hour shifts, interns with no support systems, consultants with no outlet and no supportive structure for women employee. Existing mental health apps are built for the general public and feel tone-deaf to medical professionals. Karmic AI is built from the ground up for this specific community.

## What Karmic AI Does

- **Anonymous check-ins** — doctors can log their mental state without fear of judgment or professional consequences
- **AI-powered support** — personalised suggestions based on role, workload, and emotional patterns
- **Role-aware experience** — different flows for Interns, Junior Residents, Senior Residents, and Consultants
- **Women-Safe Mode** — a dedicated support layer for women in medicine, accessible from the dashboard
- **Hospital Admin Dashboard** — anonymised department-level wellbeing data so hospitals can act on trends without exposing individuals
- **Peer Support** — connect with peers going through similar experiences

## Tech Stack

| Layer | Tech |
|---|---|
| Frontend | HTML, CSS, JavaScript |
| Backend | Java (Spring Boot) |
| API | REST (`/api/*` endpoints) |
| Deployment | Vercel |

## Project Structure

```
karmic-ai/
├── frontend/
│   ├── index.html        # Landing page
│   ├── signup.html       # Role-based signup
│   ├── dashboard.html    # User dashboard
│   ├── admin.html        # Hospital admin view
│   ├── peer.html         # Peer support
│   ├── women.html        # Women support mode
│   └── css/
└── backend/
    ├── pom.xml
    └── src/main/java/com/karmicai/
        ├── KarmicAiApplication.java
        ├── SecurityConfig.java
        ├── controller/
        ├── model/
        ├── repository/
        └── service/
```

## User Flows

**Regular Doctor:**
1. Open the live link → Get Started
2. Select role (Intern / Junior Resident / Senior Resident / Consultant)
3. Sign up → User dashboard

**Hospital Admin:**
1. On login page → toggle "Log in as Hospital Admin"
2. Anonymised department-level dashboard

**Women's Support:**
1. Log in as any user
2. Dashboard sidebar → toggle "Women-Safe Mode"
3. Access dedicated women's support page

## Run Locally (Optional)

**Frontend only:**
```bash
git clone https://github.com/samarpitad847-oss/Karmic-AI.git
open frontend/index.html
```

**With backend:**
```bash
cd backend
mvn spring-boot:run
# Runs on http://localhost:8080
```

## Hackathon Recognition

- **Top Finalist** — EliteHer Hackathon (Top 250 out of 5000+ registrations)
- Presented at multiple national-level hackathons across IITs and IIITs
