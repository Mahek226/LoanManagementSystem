package com.tss.springsecurity.ocr;

public class OCRResponse {
    private DocumentType documentType;
    private String text;

    public OCRResponse() {}

    public OCRResponse(DocumentType documentType, String text) {
        this.documentType = documentType;
        this.text = text;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
