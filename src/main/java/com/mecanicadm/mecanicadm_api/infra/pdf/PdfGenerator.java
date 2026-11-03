package com.mecanicadm.mecanicadm_api.infra.pdf;

import java.util.Map;

public interface PdfGenerator {
    byte[] generatePdfFromHtml(String templateName, Map<String, Object> variables);
}
