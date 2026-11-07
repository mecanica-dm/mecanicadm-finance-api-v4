package com.mecanicadm.mecanicadm_finance_api.shared.usecase;

public interface UseCase<I, O> {
    O execute(I input);
}
