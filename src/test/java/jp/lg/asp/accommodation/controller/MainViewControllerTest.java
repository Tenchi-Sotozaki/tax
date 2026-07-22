package jp.lg.asp.accommodation.controller;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MainViewControllerTest {

    MainViewController controller = new MainViewController();

    @Test
    void login_ログイン画面を返す() {
        assertThat(controller.login()).isEqualTo("auth/login");
    }

    @Test
    void root_リダイレクト() {
        assertThat(controller.root()).isEqualTo("redirect:/collector/list");
    }

    @Test
    void consolidatedDeclarationRegistration() {
        assertThat(controller.consolidatedDeclarationRegistration())
                .isEqualTo("declaration/consolidated-declaration-registration");
    }

    @Test
    void paymentManagement() {
        assertThat(controller.paymentManagement()).isEqualTo("declaration/payment-management");
    }
}
