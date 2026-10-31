package com.mecanicadm.mecanicadm_api.infra.features.workorder.api;

import com.mecanicadm.mecanicadm_api.core.workorder.exception.BudgetExceptions;
import com.mecanicadm.mecanicadm_api.core.workorder.usecase.ProcessBudgetDecisionByTokenUseCase;
import com.mecanicadm.mecanicadm_api.core.workorder.usecase.command.ProcessBudgetDecisionByTokenCommand;
import com.mecanicadm.mecanicadm_api.infra.features.workorder.api.openapi.BudgetDecisionPublicOpenApi;
import com.mecanicadm.mecanicadm_api.infra.features.workorder.api.renderer.BudgetDecisionPageRenderer;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/budget-decision")
public class BudgetDecisionPublicController implements BudgetDecisionPublicOpenApi {

    private static final String ACTION_APPROVED = "APPROVED";

    private final ProcessBudgetDecisionByTokenUseCase processBudgetDecisionByTokenUseCase;

    public BudgetDecisionPublicController(ProcessBudgetDecisionByTokenUseCase processBudgetDecisionByTokenUseCase) {
        this.processBudgetDecisionByTokenUseCase = processBudgetDecisionByTokenUseCase;
    }

    @Override
    @GetMapping("/{token}/form")
    public ResponseEntity<String> form(@PathVariable String token,
                                       @RequestParam String action) {
        try {
            processBudgetDecisionByTokenUseCase.validateToken(token);
        } catch (BudgetExceptions.BudgetTokenInvalid e) {
            return htmlResponse(BudgetDecisionPageRenderer.errorPage(
                    "Link já utilizado",
                    "Este link de resposta ao orçamento já foi utilizado. Caso precise alterar sua resposta, solicite um novo link à oficina."));
        } catch (BudgetExceptions.BudgetTokenNotFound e) {
            return htmlResponse(BudgetDecisionPageRenderer.errorPage(
                    "Link inválido",
                    "O link de resposta ao orçamento não foi encontrado. Verifique o link no e-mail e tente novamente."));
        }

        String title = switch (action) {
            case "REJECTED" -> "Rejeitar Orçamento";
            case "CHANGES_REQUESTED" -> "Solicitar Alterações";
            case ACTION_APPROVED -> "Aprovar Orçamento";
            default -> "Resposta ao Orçamento";
        };

        String label = switch (action) {
            case "REJECTED" -> "Informe o motivo da rejeição:";
            case "CHANGES_REQUESTED" -> "Descreva as alterações solicitadas:";
            case ACTION_APPROVED -> "Observações (opcional):";
            default -> "Observações:";
        };

        boolean required = !ACTION_APPROVED.equals(action);

        return htmlResponse(BudgetDecisionPageRenderer.formPage(title, token, action, label, required));
    }

    @Override
    @PostMapping("/{token}")
    public ResponseEntity<String> decide(@PathVariable String token,
                                         @RequestParam String action,
                                         @RequestParam(required = false) String observation) {
        try {
            processBudgetDecisionByTokenUseCase.execute(
                    new ProcessBudgetDecisionByTokenCommand(token, action, observation));
        } catch (BudgetExceptions.BudgetTokenInvalid e) {
            return htmlResponse(BudgetDecisionPageRenderer.errorPage(
                    "Link já utilizado",
                    "Este link de resposta ao orçamento já foi utilizado. Caso precise alterar sua resposta, solicite um novo link à oficina."));
        } catch (BudgetExceptions.BudgetTokenNotFound e) {
            return htmlResponse(BudgetDecisionPageRenderer.errorPage(
                    "Link inválido",
                    "O link de resposta ao orçamento não foi encontrado. Verifique o link no e-mail e tente novamente."));
        }

        return htmlResponse(BudgetDecisionPageRenderer.successPage(action));
    }

    private ResponseEntity<String> htmlResponse(String html) {
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }
}
