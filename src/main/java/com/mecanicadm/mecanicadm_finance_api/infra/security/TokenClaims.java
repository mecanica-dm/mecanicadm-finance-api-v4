package com.mecanicadm.mecanicadm_finance_api.infra.security;

public record TokenClaims(String subject, String role) {
}