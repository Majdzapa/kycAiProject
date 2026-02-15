# KYC AI Application: User Guide

Welcome to the **KYC AI Application**, a GDPR-compliant identity verification system powered by local AI. This guide will walk you through the core features with real-world examples.

---

## 1. Accessing the Application

### Initial Login
If you haven't registered yet, you can use the default administrator account to explore:
- **Username**: `admin`
- **Password**: `admin123`

### Registration
1.  Go to the **Register** page.
2.  Fill in your details (Username, Email, Password).
3.  Upon success, you will be redirected to the **Login** page.

---

## 2. Submitting a KYC Document (Example)

The core feature is submitting your identity documents for AI-powered verification.

### Real Example: Uploading a Passport
1.  **Navigate** to the **KYC Submit** section from your dashboard.
2.  **Select Document Type**: Choose `Passport` from the dropdown.
3.  **Upload File**: Drag and drop or select your passport image (JPG/PNG).
4.  **Consent**: Click the checkbox to agree to data processing under GDPR Article 6(1)(c).
5.  **Submit**: Click "Submit for Verification".

### What happens behind the scenes?
- **OCR**: The system extracts text from your passport image using Tesseract.
- **AI Analysis**: The `DocumentAgent` (llama3.2) analyzes the text for authenticity, checks the expiry date, and identifies any suspicious patterns.
- **Encryption**: Your data is encrypted before storage to ensure GDPR compliance.

---

## 3. Tracking Your Status

Go to the **KYC Status** page to see the progress of your submissions:
- ⏳ **Pending**: AI is currently analyzing your document.
- ✅ **Verified**: The AI has confirmed the document is valid and high-quality.
- ⚠️ **Needs Review**: The AI detected low confidence or potential issues that require a human operator's attention.
- ❌ **Rejected**: The document is expired or invalid.

---

## 4. Interacting with the AI Chatbot

The AI Chatbot is designed to help you with the KYC process and GDPR inquiries using **RAG (Retrieval-Augmented Generation)**.

### Example Interaction 1: Process Help
**User**: *"Which documents are accepted for proof of address?"*  
**AI Chatbot**: *"For proof of address, we accept utility bills (electricity, water, gas), bank statements, or government correspondence issued within the last 3 months."*

### Example Interaction 2: GDPR Inquiry
**User**: *"What are my rights regarding my data?"*  
**AI Chatbot**: *"Under GDPR, you have the right to access your data, the right to rectification, the right to erasure ('right to be forgotten'), and the right to data portability. You can exercise these in the GDPR settings section."*

---

## 5. GDPR Privacy Controls

Protecting your data is our priority. You can find these tools in the **GDPR** section:
- **Data Export**: Download a full JSON report of all personal data the system holds about you.
- **Right to be Forgotten**: Request the permanent deletion or anonymization of your records.
- **Consent Management**: View and update your data processing consents.

---

## 6. Admin Dashboard (Operators Only)

If you are logged in as an **Admin** or **Operator**, you will see an extra tab:
- **System Metrics**: Real-time monitoring of AI processing and server health.
- **Manual Review**: Approve or reject documents that the AI flagged as "Needs Review".
- **Knowledge Base**: Upload regulatory documents to keep the AI Chatbot updated with the latest laws.
