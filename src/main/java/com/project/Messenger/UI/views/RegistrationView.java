package com.project.Messenger.UI.views;

import com.project.Messenger.backend.data.service.RegisterService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import org.springframework.stereotype.Component;

@Route("registration")
@PageTitle("Registration")
@UIScope
@Component
public class RegistrationView extends VerticalLayout {

    public RegistrationView(RegisterService registerService) {
        // Заголовок
        H1 title = new H1("Registration");
        title.getStyle().set("color", "grey");

        // Поля ввода
        TextField username = new TextField("Username");
        PasswordField password = new PasswordField("Password");
        PasswordField confirmPassword = new PasswordField("Confirm Password");
        Button submitButton = new Button("Submit");

        // Настройки стилей
        username.setWidth("300px");
        password.setWidth("300px");
        confirmPassword.setWidth("300px");
        submitButton.setWidth("300px");

        // Установить стили для полей и кнопки
        username.getStyle().set("max-width", "300px");
        password.getStyle().set("max-width", "300px");
        confirmPassword.getStyle().set("max-width", "300px");
        submitButton.getStyle().set("max-width", "300px");

        // Центрирование и масштабируемость
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setHeightFull();

        // Установка стиля фона
        getElement().getStyle().set("background", "linear-gradient(to right, #3a1c71, #d76d77, #ffaf7b)");
        getElement().getStyle().set("min-height", "100vh");
        getElement().getStyle().set("display", "flex");
        getElement().getStyle().set("flex-direction", "column");
        getElement().getStyle().set("align-items", "center");
        getElement().getStyle().set("justify-content", "center");

        // Добавление компонентов в макет
        add(title, username, password, confirmPassword, submitButton);

        // Обработчик кнопки
        submitButton.addClickListener(event -> {
            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Notification.show("All fields must be filled out");
            } else if (!password.getValue().equals(confirmPassword.getValue())) {
                Notification.show("Passwords do not match");
            } else {
                // Если все проверки пройдены, записываем в бд и перенаправляем на страницу входа
                try {
                    registerService.register(username.getValue(), password.getValue());

                    username.clear();
                    password.clear();
                    confirmPassword.clear();

                    getUI().ifPresent(ui -> ui.navigate("login"));
                } catch (RegisterService.RegisterException e) {
                    Notification.show("Such username already exists!");
                }
            }
        });
    }
}
