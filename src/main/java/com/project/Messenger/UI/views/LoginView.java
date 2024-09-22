package com.project.Messenger.UI.views;

import com.project.Messenger.backend.data.service.AuthService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;

import javax.management.Notification;

@Route("login")
@PageTitle("Safe Messenger")
public class LoginView extends VerticalLayout {
    LoginForm MainForm = new LoginForm();

    public LoginView(AuthService authService) {

        // Свойства для layout
        setSizeFull();
        setSpacing(true);
        setMargin(false);
        getElement().getStyle().set("background", "linear-gradient(to right, #3a1c71, #d76d77, #ffaf7b)");
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);


        // Кнопки на самой форме
        MainForm.addForgotPasswordListener(event -> {
            getUI().ifPresent(ui -> ui.navigate("registration"));
        });

        MainForm.addLoginListener(event -> {
            try {
                long personId = authService.authenticate(event.getUsername(), event.getPassword());

                getUI().ifPresent(ui -> ui.navigate(PersonalView.class, new RouteParameters("id", String.valueOf(personId))));
            } catch (AuthService.InvalidPasswordException e) {
                MainForm.showErrorMessage("Invalid password!", "Please, try another password.");
            } catch (AuthService.InvalidUserException e) {
                MainForm.showErrorMessage("Invalid username!", "Please, try another username.");
            } catch (AuthService.AuthException e) {
                MainForm.showErrorMessage("Auth fail!", "Please, try again.");
            }
        });


        // Текстовые приколюхи
        LoginI18n i18n = LoginI18n.createDefault();
        i18n.getForm().setTitle("Messenger | Safe and fast");
        i18n.getForm().setForgotPassword("Don't have an account? Register!");
        i18n.getErrorMessage().setTitle("Invalid credentials");
        i18n.setAdditionalInformation("Please, contact dolgofff@company.com if you're experiencing issues logging into your account.");
        MainForm.setI18n(i18n);

        // Устанавливаем цвета для формы входа
        MainForm.getElement().getStyle().set("border-radius", "10px"); // Радиус скругления углов
        MainForm.getElement().getStyle().set("box-shadow", "0 4px 8px rgba(0, 0, 0, 0.1)"); // Тень
        MainForm.getElement().getStyle().set("color", "black"); // Белый цвет текста
        MainForm.getElement().getThemeList().add("tertiary");

        add(MainForm);
    }
}
