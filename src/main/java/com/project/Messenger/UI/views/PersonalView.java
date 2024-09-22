package com.project.Messenger.UI.views;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.Messenger.UI.views.PageUpdater.Broadcaster;
import com.project.Messenger.backend.broker.KafkaMessage;
import com.project.Messenger.backend.broker.dialogueLogic.MessageProducer;
import com.project.Messenger.backend.broker.serialization.SerializationParser;
import com.project.Messenger.backend.broker.serialization.SerializedMessage;
import com.project.Messenger.backend.data.model.Room;
import com.project.Messenger.backend.data.model.User;
import com.project.Messenger.backend.data.service.KafkaMessageService;
import com.project.Messenger.backend.data.service.RoomService;
import com.project.Messenger.backend.data.service.UserService;
import com.project.Messenger.backend.encryption.implementations.RC5.RC5;
import com.project.Messenger.backend.encryption.implementations.Twofish.Twofish;
import com.project.Messenger.backend.encryption.implementations.modes.*;
import com.project.Messenger.backend.encryption.implementations.paddings.PaddingService;
import com.project.Messenger.backend.encryption.implementations.paddings.impl.ANSIx923;
import com.project.Messenger.backend.encryption.implementations.paddings.impl.ISO10126;
import com.project.Messenger.backend.encryption.implementations.paddings.impl.PKCS7;
import com.project.Messenger.backend.encryption.implementations.paddings.impl.Zeros;
import com.project.Messenger.backend.encryption.interfaces.ICipher;
import com.project.Messenger.backend.encryption.interfaces.IModeCipher;
import com.project.Messenger.backend.server.Server;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.StreamResource;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Route("personal/:id")
@PageTitle("Dialogues")
@Slf4j
public class PersonalView extends HorizontalLayout implements BeforeEnterObserver {


    //Backend fields
    private long id;
    KafkaMessageService kafkaMessageService;
    private MessageProducer messageProducer = new MessageProducer();
    private KafkaConsumer<String, String> kafkaConsumer;
    private ExecutorService messageThreadExecutor; //сервис для управления задач с сообщениями в отдельном потоке.
    private SerializationParser serializationParser = new SerializationParser();
    Backend backend;
    private Room room;
    private User user;
    private final Server server;
    private volatile boolean processingMessages = false;
    private final UI currentUI = UI.getCurrent();
    List<String> currentCompanions = new ArrayList<>();
    private final UserService userService;
    private final RoomService roomService;

    //dh utils
    private byte[] myPrivateKey;
    private byte[] myPublicNumber;
    private byte[] companionsPublicNumber;
    private byte[] sharedSecret;

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        Optional<Long> getId = beforeEnterEvent.getRouteParameters().getLong("id");
        if (getId.isPresent()) {
            Optional<User> getUser = userService.getUserByPersonId(getId.get());

            if (getUser.isPresent()) {
                this.user = getUser.get();
                this.backend = new Backend();

                if (user.getConnectedRooms() != null) {
                    chatList.setItems(backend.updateDialoguesBeforeEntering());
                }
            }
        }
    }

    private void handleBroadcastMessage(UI ui, String message) {
        if ("updateGrid".equals(message)) {
            ui.access(() -> ui.navigate(ui.getInternals().getActiveViewLocation().getPath()));
        }

        if ("DeleteRoom".equals(message)) {
            stopMessagesThread();
            ui.access(() -> ui.navigate(ui.getInternals().getActiveViewLocation().getPath()));
        }
    }

    @Override
    protected void onDetach(DetachEvent event) {
        Broadcaster.unregister(this::handleBroadcastMessage);
        super.onDetach(event);
    }

    //Frontend fields
    String selectedChat;
    Span displayCompanionsName;
    private Grid<String> chatList;
    private VerticalLayout chatWindow = new VerticalLayout();
    private VerticalLayout messagesLayout = new VerticalLayout();
    private TextField messageInput = new TextField();
    private Button sendButton = new Button(VaadinIcon.PAPERPLANE.create());
    private Button uploadButton = new Button(VaadinIcon.PAPERCLIP.create());
    private Button logoutButton = new Button(VaadinIcon.SIGN_OUT.create());
    private Button createChatButton = new Button("Create New Chat", VaadinIcon.PLUS.create());

    private Dialog createChatDialog = new Dialog(); //Диалог для создания нового чата

    private VerticalLayout chatListLayout;
    private HorizontalLayout headerLayout;

    private List<VerticalLayout> messageComponents = new ArrayList<>();

    public PersonalView(Server server, UserService userService, RoomService roomService, KafkaMessageService kafkaMessageService) {
        //Для бэка
        this.server = server;
        this.roomService = roomService;
        this.userService = userService;
        this.kafkaMessageService = kafkaMessageService;

        //Подписываемся на обновления
        Broadcaster.register(this::handleBroadcastMessage, UI.getCurrent());


        //Настройка стилей основной панели
        setHeightFull();
        setWidthFull();
        setPadding(false);
        setSpacing(true);

        Icon logo = VaadinIcon.ROCKET.create();
        logo.setSize("24px");

        Span chatTitle = new Span("Chats");
        chatTitle.getElement().getStyle().set("font-size", "20px");  // Размер текста заголовка
        chatTitle.getElement().getStyle().set("font-weight", "bold");  // Жирный шрифт
        chatTitle.getElement().getStyle().set("margin-left", "10px");

        logoutButton.addClickListener(e -> {
            cleanup();
            getUI().ifPresent(ui -> ui.navigate("login"));
        });

        // Верхняя панель с логотипом, текстом и кнопкой выхода
        headerLayout = new HorizontalLayout(logo, chatTitle, logoutButton);
        headerLayout.setWidthFull();
        headerLayout.setPadding(false);
        headerLayout.setAlignItems(Alignment.CENTER);
        headerLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        headerLayout.addClassName("header-layout");

        // Добавление округлых краёв для headerLayout
        headerLayout.getElement().getStyle().set("border-radius", "15px");
        headerLayout.getElement().getStyle().set("background-color", "#f5f5f5");
        headerLayout.getElement().getStyle().set("padding", "10px");
        headerLayout.getElement().getStyle().set("border", "1px solid #e0e0e0");

        chatList = new Grid<>(String.class);
        chatList.removeAllColumns();
        chatList.getElement().getStyle().set("background-color", "#f5f5f5");
        chatList.addColumn(s -> s).setHeader("Username");
        chatList.setSelectionMode(Grid.SelectionMode.SINGLE);
        chatList.addClassName("chat-list");

        displayCompanionsName = new Span("Welcome!");
        displayCompanionsName.getElement().getStyle().set("font-size", "18px");  // Размер текста
        displayCompanionsName.getElement().getStyle().set("font-weight", "bold");  // Жирный шрифт
        displayCompanionsName.getElement().getStyle().set("margin-left", "10px");

        chatList.addItemClickListener(event -> {
            //Проверяем, была ли до этого открыта какая-нибудь комната, если да -> закрываем её поток
            if (room != null) {
                stopMessagesThread();
                backend.cutConnectionWithRoom(room.getRoomId());
            }

            selectedChat = event.getItem(); //Получаем выбранное имя собеседника
            room = backend.getRoomByUsername(selectedChat, user.getPersonId());

            if (room != null) {
                displayCompanionsName.setText(selectedChat);
                backend.setConnectionWithRoom(room.getRoomId());
                consumerThread();
                handleIncomingMessages();
                loadConversationWithCompanion();
                myPrivateKey = Backend.generatePrivateKey(room.getP());
                log.info("##### my private key was generated: " + Arrays.toString(myPrivateKey));
                myPublicNumber = Backend.generatePublicKey(room.getP(), room.getG(), myPrivateKey);
                log.info("#### my public number was generated:" + Arrays.toString(myPublicNumber));

                //как только оба типа будут в комнате, то последний зашедший отправляет запрос об обмене числами
                //второй тип читает его и отправляет в ответ своё число
                if (roomService.isBothActive(room.getRoomId())) {
                    SerializedMessage exchangeKeysNotification = SerializedMessage.builder()
                            .roomId(room.getRoomId())
                            .content(myPublicNumber)
                            .senderId(user.getPersonId())
                            .messageIdentity("ExchangeKeys")
                            .build();

                    try {
                        messageProducer.sendMessage(String.format("messageStorageOfChat-%s", room.getRoomId()), serializationParser.processJsonMessage(exchangeKeysNotification));
                        log.info("##### ExchangeKeys notification sent ##### in topic: " + String.format("messageStorageOfChat-%s", room.getRoomId()) + "by: " + exchangeKeysNotification.getSenderId() + "in room:" + exchangeKeysNotification.getRoomId());
                    } catch (JsonProcessingException exc) {
                        throw new RuntimeException(exc);
                    }
                }
            } else {
                Notification.show("This room is already empty!");
            }
        });

        // Настройка кнопки создания нового чата
        createChatButton.addClickListener(e -> openCreateChatDialog());
        createChatButton.setWidthFull();
        createChatButton.getElement().getStyle().set("border-radius", "5px");
        createChatButton.getElement().getStyle().set("background-color", "#007bff");
        createChatButton.getElement().getStyle().set("color", "#ffffff");
        createChatButton.getElement().getStyle().set("padding", "10px");

        // Добавление списка чатов и кнопок в вертикальный лейаут
        chatListLayout = new VerticalLayout(headerLayout, chatList);
        chatListLayout.setPadding(false);
        chatListLayout.setSpacing(true);  // Отступы между элементами
        chatListLayout.setWidth("280px");  // Уменьшение ширины списка чатов
        chatListLayout.setHeight("90vh");  // Высота списка чатов, чтобы совпадала с формой чата
        chatListLayout.addClassName("chat-list-layout");
        chatListLayout.getElement().getStyle().set("border-radius", "15px"); // Округлые края
        chatListLayout.getElement().getStyle().set("background-color", "#ffffff");
        chatListLayout.getElement().getStyle().set("border", "1px solid #e0e0e0");
        chatListLayout.getElement().getStyle().set("padding", "10px");

        // Добавление кнопки создания чата внизу
        chatListLayout.add(createChatButton);
        chatListLayout.setAlignItems(Alignment.STRETCH);
        chatListLayout.expand(chatList);  // Раздвигаем список чатов по всей высоте

        // Настройка заголовка для диалога (иконка пользователя, имя, кнопка удаления чата)
        Icon userIcon = VaadinIcon.USER.create();
        userIcon.setSize("24px");

        Span userName = new Span("");
        userName.getElement().getStyle().set("font-size", "18px");  // Размер текста
        userName.getElement().getStyle().set("font-weight", "bold");  // Жирный шрифт
        userName.getElement().getStyle().set("margin-left", "10px");

        Button deleteChatButton = new Button(VaadinIcon.TRASH.create());
        deleteChatButton.getElement().getStyle().set("background-color", "#dde38d");

        deleteChatButton.addClickListener(e -> {
            if (room == null) {
                Notification.show("You have no room to delete!");
            } else {
                if (!roomService.isBothActive(room.getRoomId())) {
                    Notification.show("You cannot delete this room while your companion is not connected!");
                } else {
                    messagesLayout.removeAll();
                    currentCompanions.remove(displayCompanionsName.getText());
                    displayCompanionsName.setText("Welcome!");
                    //stopMessagesThread();
                    kafkaMessageService.deleteAllMessagesByRoomId(room.getRoomId());
                    long tempRoomId = room.getRoomId();
                    backend.executeRoom(room.getRoomId());
                    room = null;
                    //отправка события об удалении у второго типули
                    KafkaMessage deleteRoomNotification = KafkaMessage.builder()
                            .roomId(tempRoomId)
                            .messageIdentity("DeleteRoom")
                            .senderId(user.getPersonId())
                            .build();

                    SerializedMessage serializedMessage = backend.serializeMessage(deleteRoomNotification);

                    try {
                        messageProducer.sendMessage(String.format("messageStorageOfChat-%s", deleteRoomNotification.getRoomId()), serializationParser.processJsonMessage(serializedMessage));
                    } catch (JsonProcessingException exc) {
                        throw new RuntimeException(exc);
                    }
                }
            }
        });

        // Верхняя панель с иконкой пользователя, именем и кнопкой удаления чата
        HorizontalLayout chatHeader = new HorizontalLayout(userIcon, displayCompanionsName, deleteChatButton);
        chatHeader.setWidthFull();
        chatHeader.setPadding(false);
        chatHeader.setAlignItems(Alignment.CENTER);
        chatHeader.setJustifyContentMode(JustifyContentMode.BETWEEN);
        chatHeader.getElement().getStyle().set("padding", "10px");
        chatHeader.getElement().getStyle().set("border-bottom", "1px solid #e0e0e0");
        chatHeader.getElement().getStyle().set("background-color", "#ccc8c8");
        chatHeader.getElement().getStyle().set("border-radius", "15px 15px 0 0");

        // Настройка основного окна чата
        chatWindow.setWidth("calc(100% - 305px)");  // Уменьшение ширины основного окна чата
        chatWindow.setHeight("90vh");  // Высота окна чата
        chatWindow.addClassName("chat-window");

        // Поле для отображения сообщений
        messagesLayout.setWidthFull();
        messagesLayout.setPadding(false);
        messagesLayout.setSpacing(false);
        messagesLayout.addClassName("messages-layout");

        uploadButton.addClickListener(event -> {
            uploadButton.setEnabled(true);

            // Настройка диалогового окна для загрузки файлов
            MemoryBuffer memoryBuffer = new MemoryBuffer();
            Upload upload = new Upload(memoryBuffer);
            upload.setMaxFiles(1);
            upload.setMaxFileSize(10485760);
            Dialog uploadDialog = new Dialog();
            uploadDialog.add(upload);
            uploadDialog.open();
            upload.addSucceededListener(uploadEvent -> {
                String nameOfFile = uploadEvent.getFileName();
                String mimeType = uploadEvent.getMIMEType();
                String typeOfFile = mimeType.split("/")[1];

                try {
                    byte[] bytes = backend.handleInputStream(memoryBuffer.getInputStream());
                    if (typeOfFile.equals("png") || typeOfFile.equals("jpg") || typeOfFile.equals("jpeg")) {
                        sendMessageWithImage(bytes);
                        uploadDialog.close();
                    } else {
                        sendMessageWithFile(bytes, nameOfFile);
                        uploadDialog.close();
                    }

                } catch (IOException e) {
                    log.error(e.getMessage());
                    uploadDialog.close();
                    Notification.show("Please, try again!");
                }
            });
            upload.addFileRejectedListener(fileRejectedEvent -> Notification.show("Biggest possible size is 10MB"));
            uploadButton.setEnabled(true);
        });

        // Настройка кнопки отправки сообщения
        sendButton.addClickListener(e -> {
            if (room == null) {
                Notification.show("Please, choose your companion");
                messagesLayout.removeAll();
                return;
            }
            if (messageInput.getValue().isEmpty()) {
                Notification.show("Please, enter your message");
                return;
            }

            if (roomService.isBothActive(room.getRoomId())) {
                try {
                    sendMessageWithText(messageInput.getValue());
                    Notification.show("Message sent!");
                } catch (JsonProcessingException ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                Notification.show("Your companion must be connected to this room!");
            }

            messageInput.clear();
        });
        sendButton.addClassName("send-button");

        // Упорядочивание поля ввода и кнопок в горизонтальный лейаут
        HorizontalLayout inputLayout = new HorizontalLayout(messageInput, uploadButton, sendButton);
        inputLayout.setWidthFull();
        inputLayout.setPadding(true);
        inputLayout.setAlignItems(Alignment.CENTER);
        inputLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        inputLayout.getElement().getStyle().set("padding", "10px");

        // Настройка поля ввода сообщения с Placeholder
        messageInput.setPlaceholder("Type your message");
        messageInput.setWidthFull();
        messageInput.getElement().getStyle().set("border-radius", "20px");

        // Добавление элементов в окно чата
        chatWindow.add(chatHeader, messagesLayout, inputLayout);
        chatWindow.setFlexGrow(1, messagesLayout);  // Сообщения занимают всё доступное пространство

        // Добавление списка чатов и основного окна чата в основной лейаут
        add(chatListLayout, chatWindow);

        // Применение стилей
        applyStyles();

        // Настройка диалогового окна для создания нового чата
        setupCreateChatDialog();
    }

    //Логика для загрузки сообщений с данным собеседником из бд
    private void loadConversationWithCompanion() {
        messagesLayout.removeAll();

        List<KafkaMessage> collectMessagesFromRoom = kafkaMessageService.getMessagesByRoomId(room.getRoomId());
        for (KafkaMessage message : collectMessagesFromRoom) {
            try {
                if (!message.getMessageIdentity().equals("ImageMessage")) {
                    message.setContent(Backend.applyHash(message.getContent(),
                            Backend.generateHash(message.getRoomId())));
                }
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }
        User companion = null;

        for (KafkaMessage message : collectMessagesFromRoom) {
            if (message.getSenderId() != user.getPersonId()) {
                if (companion == null) {
                    companion = userService.getUserByPersonId(message.getSenderId()).get();
                }
                addMessage(companion.getUsername(), message);
            } else {
                addMessage(user.getUsername(), message);
            }
        }
    }

    private void sendMessageWithText(String message) throws JsonProcessingException {
        byte[] initialMessageToBytes = message.getBytes(StandardCharsets.UTF_8);
        PaddingService padding = backend.initializePadding();
        ICipher cipher = backend.initializeCipher(sharedSecret);
        IModeCipher mode = backend.initializeMode();
        byte[] encryptedMessage = mode.encrypt(padding.applyPadding(initialMessageToBytes, room.getBlockSizeInBits() / 8),
                room.getInitializationVector(), cipher, room.getBlockSizeInBits() / 8);

        //Создаём объект сообщения и отправляем его через Kafka
        KafkaMessage messageToSend = KafkaMessage.builder()
                .messageIdentity("TextMessage")
                .content(initialMessageToBytes)
                .senderId(user.getPersonId())
                .roomId(room.getRoomId())
                .timestamp(LocalDateTime.now()).build();


        messageToSend = kafkaMessageService.save(messageToSend);
        SerializedMessage serializedMessage = backend.serializeMessage(messageToSend);
        serializedMessage.setContent(encryptedMessage);
        addMessage(user.getUsername(), messageToSend);

        try {
            messageToSend.setContent(Backend.applyHash(messageToSend.getContent(),
                    Backend.generateHash(messageToSend.getRoomId())));
            kafkaMessageService.deleteMessageById(messageToSend.getId(), messageToSend.getRoomId());
            kafkaMessageService.save(messageToSend);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }

        messageProducer.sendMessage(String.format("messageStorageOfChat-%s", messageToSend.getRoomId()), serializationParser.processJsonMessage(serializedMessage));
    }

    private void sendMessageWithImage(byte[] imageToBytes) throws JsonProcessingException {
        PaddingService padding = backend.initializePadding();
        ICipher cipher = backend.initializeCipher(sharedSecret);
        IModeCipher mode = backend.initializeMode();
        byte[] encryptedMessage = mode.encrypt(padding.applyPadding(imageToBytes, room.getBlockSizeInBits() / 8),
                room.getInitializationVector(), cipher, room.getBlockSizeInBits() / 8);

        KafkaMessage messageToSend = KafkaMessage.builder()
                .messageIdentity("ImageMessage")
                .content(imageToBytes)
                .senderId(user.getPersonId())
                .roomId(room.getRoomId())
                .timestamp(LocalDateTime.now()).build();

        messageToSend = kafkaMessageService.save(messageToSend);
        SerializedMessage serializedMessage = backend.serializeMessage(messageToSend);
        serializedMessage.setContent(encryptedMessage);
        addMessage(user.getUsername(), messageToSend);

        /*try {
            messageToSend.setContent(Backend.applyHash(messageToSend.getContent(),
                    Backend.generateHash(messageToSend.getRoomId())));
            kafkaMessageService.deleteMessageById(messageToSend.getId(), messageToSend.getRoomId());
            kafkaMessageService.save(messageToSend);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }*/

        messageProducer.sendMessage(String.format("messageStorageOfChat-%s", messageToSend.getRoomId()), serializationParser.processJsonMessage(serializedMessage));
    }

    private void sendMessageWithFile(byte[] fileBytes, String filename) throws JsonProcessingException {
        PaddingService padding = backend.initializePadding();
        ICipher cipher = backend.initializeCipher(sharedSecret);
        IModeCipher mode = backend.initializeMode();
        byte[] encryptedMessage = mode.encrypt(padding.applyPadding(fileBytes, room.getBlockSizeInBits() / 8),
                room.getInitializationVector(), cipher, room.getBlockSizeInBits() / 8);

        KafkaMessage messageToSend = KafkaMessage.builder()
                .messageIdentity(filename)
                .content(fileBytes)
                .senderId(user.getPersonId())
                .roomId(room.getRoomId())
                .timestamp(LocalDateTime.now()).build();

        messageToSend = kafkaMessageService.save(messageToSend);
        SerializedMessage serializedMessage = backend.serializeMessage(messageToSend);
        serializedMessage.setContent(encryptedMessage);
        addMessage(user.getUsername(), messageToSend);

        try {
            messageToSend.setContent(Backend.applyHash(messageToSend.getContent(),
                    Backend.generateHash(messageToSend.getRoomId())));
            kafkaMessageService.deleteMessageById(messageToSend.getId(), messageToSend.getRoomId());
            kafkaMessageService.save(messageToSend);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }

        messageProducer.sendMessage(String.format("messageStorageOfChat-%s", messageToSend.getRoomId()), serializationParser.processJsonMessage(serializedMessage));
    }

    private void addMessage(String sender, KafkaMessage kafkaMessage) {
        Span senderSpan = new Span(sender);
        Span messageSpan = new Span();
        VerticalLayout messageLayout;

        String identity = kafkaMessage.getMessageIdentity();
        if (identity.equals("TextMessage")) {
            messageLayout = new VerticalLayout(senderSpan, messageSpan);
        } else {
            messageLayout = new VerticalLayout(senderSpan);
        }


        if (identity.equals("TextMessage")) {
            String content = new String(kafkaMessage.getContent(), StandardCharsets.UTF_8);
            messageSpan.setText(content);
        } else if (identity.equals("ImageMessage")) {
            StreamResource resource = new StreamResource(
                    "image", () -> new ByteArrayInputStream(kafkaMessage.getContent())
            );
            Image image = new Image(resource, "Generated Image");
            image.addClassName("message-image");
            image.setMaxWidth("500px");
            messageLayout.add(image);
        } else {
            StreamResource resourceFile = new StreamResource(kafkaMessage.getMessageIdentity(),
                    () -> new ByteArrayInputStream(kafkaMessage.getContent())
            );
            Anchor downloadFileAnchor = new Anchor(resourceFile, kafkaMessage.getMessageIdentity());
            downloadFileAnchor.getElement().setAttribute("download", true);
            downloadFileAnchor.setText(kafkaMessage.getMessageIdentity());
            downloadFileAnchor.addClassName("message-link");
            messageLayout.add(downloadFileAnchor);
        }

        //Стили для имени отправителя
        senderSpan.getElement().getStyle().set("font-weight", "bold");
        senderSpan.getElement().getStyle().set("margin-bottom", "5px");

        if (user.getUsername().equals(sender)) {
            //Если сообщение отправлено мной (синие сообщения слева)
            messageSpan.getElement().getStyle().set("background-color", "#007bff");
            messageSpan.getElement().getStyle().set("color", "#ffffff");
            messageLayout.setAlignItems(Alignment.START);
            messageLayout.setId(String.format("%d", kafkaMessage.getId())); // Устанавливаем ID сообщения как ID компонента
        } else {
            //Если сообщение от другого пользователя (серые сообщения справа)
            messageSpan.getElement().getStyle().set("background-color", "#e0e0e0");
            messageSpan.getElement().getStyle().set("color", "#000000");
            messageLayout.setAlignItems(Alignment.END);
            messageLayout.setId(String.format("%d", kafkaMessage.getId())); // Устанавливаем ID сообщения как ID компонента
        }

        //Стили для сообщения
        messageSpan.getElement().getStyle().set("padding", "10px");
        messageSpan.getElement().getStyle().set("border-radius", "15px"); //округлые края сообщений
        messageSpan.getElement().getStyle().set("margin-bottom", "10px");
        messageSpan.getElement().getStyle().set("margin-top", "5px");
        messageSpan.getElement().getStyle().set("width", "auto");
        messageSpan.getElement().getStyle().set("display", "inline-block");

        //Создание контекстного меню для сообщения
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.setTarget(messageLayout);
        contextMenu.addItem("Delete message", e -> {
            long deletedMessageId = Long.parseLong(messageLayout.getId().orElse("0"));
            messagesLayout.remove(messageLayout);

            SerializedMessage deleteMessageNotification = SerializedMessage.builder()
                    .roomId(room.getRoomId())
                    .id(deletedMessageId)
                    .senderId(user.getPersonId())
                    .messageIdentity("DeleteMessage")
                    .build();

            try {
                messageProducer.sendMessage(String.format("messageStorageOfChat-%s", room.getRoomId()), serializationParser.processJsonMessage(deleteMessageNotification));  // Отправляем событие удаления
            } catch (JsonProcessingException exc) {
                throw new RuntimeException(exc);
            }
            log.info("###################### message with id " + deletedMessageId + " was deleted ############");
            kafkaMessageService.deleteMessageById(deletedMessageId, room.getRoomId());
            //  kafkaMessageService.deleteMessageById(kafkaMessage.getId(), room.getRoomId());
        });

        messagesLayout.add(messageLayout);
        messageComponents.add(messageLayout);
    }

    private void handleIncomingMessages() {
        //отправляем задачу на выполнение в потоке(асинхронно обрабатываем сообщения)
        messageThreadExecutor.submit(() -> {
            try {
                SerializationParser parser = new SerializationParser();
                processingMessages = true;
                while (processingMessages) {
                    try {
                        ConsumerRecords<String, String> records = kafkaConsumer.poll(100);
                        for (ConsumerRecord<String, String> r : records) {
                            String recorded = r.value();
                            SerializedMessage serializedMessage = parser.processStringMessage(recorded);

                            if (serializedMessage.getSenderId() != user.getPersonId()) {
                                if (serializedMessage.getMessageIdentity().equals("DeleteMessage")) {
                                    // Находим сообщение по ID и удаляем его из messagesLayout
                                    currentUI.access(() -> {
                                        messagesLayout.getChildren()
                                                .filter(component -> component.getId().orElse(null).equals(String.format("%d", serializedMessage.getId())))
                                                .findFirst()
                                                .ifPresent(messagesLayout::remove);
                                        log.info("###################### message with id " + serializedMessage.getId() + " was deleted (foreigner) ############");
                                    });
                                } else if (serializedMessage.getMessageIdentity().equals("DeleteRoom")) {
                                    currentUI.access(() -> {
                                        if (room != null && room.getRoomId() == serializedMessage.getRoomId()) {
                                            room = null;
                                            //stopMessagesThread();
                                            messagesLayout.removeAll();
                                            currentCompanions.remove(displayCompanionsName.getText());
                                            displayCompanionsName.setText("Welcome!");
                                            Notification.show("This room was deleted by your companion.");
                                        }
                                        Broadcaster.broadcast("DeleteRoom");
                                    });
                                } else if (serializedMessage.getMessageIdentity().equals("ExchangeKeys")) {
                                    log.info("holaaaaaaaaaaaaaaa");
                                    currentUI.access(() -> {
                                        companionsPublicNumber = serializedMessage.getContent();
                                        log.info("######## Received companions public number: " + Arrays.toString(companionsPublicNumber) + "###############");
                                        sharedSecret = Backend.computeSharedSecret(companionsPublicNumber, myPrivateKey, room.getP());
                                        log.info("#################### Computed shared secret: " + Arrays.toString(sharedSecret) + "####################");

                                        SerializedMessage exchangeKeysAnswerNotification = SerializedMessage.builder()
                                                .roomId(room.getRoomId())
                                                .content(myPublicNumber)
                                                .senderId(user.getPersonId())
                                                .messageIdentity("ExchangeKeysAnswer")
                                                .build();
                                        try {
                                            messageProducer.sendMessage(String.format("messageStorageOfChat-%s", room.getRoomId()), serializationParser.processJsonMessage(exchangeKeysAnswerNotification));
                                        } catch (JsonProcessingException exc) {
                                            throw new RuntimeException(exc);
                                        }
                                    });
                                } else if (serializedMessage.getMessageIdentity().equals("ExchangeKeysAnswer")) {
                                    currentUI.access(() -> {
                                        companionsPublicNumber = serializedMessage.getContent();
                                        sharedSecret = Backend.computeSharedSecret(companionsPublicNumber, myPrivateKey, room.getP());
                                        log.info("#################### Computed shared secret(foreigner): " + Arrays.toString(sharedSecret) + "####################");
                                    });
                                } else {
                                    PaddingService padding = backend.initializePadding();
                                    ICipher cipher = backend.initializeCipher(sharedSecret);
                                    IModeCipher mode = backend.initializeMode();

                                    serializedMessage.setContent(padding.removePadding(
                                            mode.decrypt(serializedMessage.getContent(), room.getInitializationVector(),
                                                    cipher, room.getBlockSizeInBits() / 8)));

                                    KafkaMessage kafkaMessage = backend.deserializeMessage(serializedMessage);
                                    currentUI.access(() -> {
                                                boolean isMessageFound = messagesLayout.getChildren()
                                                        .anyMatch(component -> component.getId()
                                                                .orElse(null).equals(String.format("%d", serializedMessage.getId())));

                                                if (!isMessageFound) {
                                                    addMessage(userService.getUserByPersonId(kafkaMessage.getSenderId()).get().getUsername(), kafkaMessage);
                                                    log.info("######### added companions message with id: " + kafkaMessage.getId());
                                                }
                                            }
                                    );
                                }
                            }
                        }
                    } catch (WakeupException e) {
                        // Если poll был прерван через wakeup(), выходим из цикла
                        if (!processingMessages) {
                            break; // Выходим из цикла обработки сообщений
                        }
                        throw e; // Пробрасываем исключение, если оно не связано с остановкой
                    }
                }
            } catch (Exception exc) {
                log.error(exc.getMessage(), exc);
            } finally {
                if (kafkaConsumer != null) {
                    kafkaConsumer.close();
                    kafkaConsumer = null;
                }
            }
        });
    }

    private KafkaConsumer<String, String> setKafkaConsumerConfiguration(String topic, String groupId) {
        Properties props = new Properties();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9093");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, "10485760");
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(topic));

        return consumer;
    }

    private void setKafkaConsumerParameters() {
        kafkaConsumer = setKafkaConsumerConfiguration(String.format("messageStorageOfChat-%s", room.getRoomId()),
                String.format("consumerGroup-%s", user.getPersonId()));
    }

    private void consumerThread() {
        //создаём новый поток для обработки сообщений из кафки
        messageThreadExecutor = Executors.newSingleThreadExecutor();
        setKafkaConsumerParameters();
    }

    private void stopMessagesThread() {
        processingMessages = false;
        if (kafkaConsumer != null) {
            kafkaConsumer.wakeup(); // Прерываем операцию poll()
        }
        messageThreadExecutor.shutdown(); // Остановить ExecutorService
        try {
            if (!messageThreadExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                messageThreadExecutor.shutdownNow();  // Принудительная остановка, если поток не завершился
            }
        } catch (InterruptedException e) {
            messageThreadExecutor.shutdownNow();
            Thread.currentThread().interrupt();  // Восстановить статус прерывания
        } finally {
            if (kafkaConsumer != null) {
                kafkaConsumer.close(); // Закрыть consumer после завершения потока
                kafkaConsumer = null;
            }
        }
    }

    private void setupCreateChatDialog() {
        TextField userIdField = new TextField("User ID");

        ComboBox<String> cipherComboBox = new ComboBox<>("Cipher");
        cipherComboBox.setItems("Twofish", "RC5");

        ComboBox<String> cipherModeComboBox = new ComboBox<>("Cipher Mode");
        cipherModeComboBox.setItems("CBC", "CFB", "CTR", "ECB", "OFB", "PCBC", "Random Delta");

        ComboBox<String> paddingComboBox = new ComboBox<>("Padding");
        paddingComboBox.setItems("ANSIx923", "ISO10126", "PKCS7", "Zeros");

        Button createButton = new Button("Create Chat", VaadinIcon.PLUS.create());
        createButton.getElement().getStyle().set("background-color", "#007bff"); // Синий цвет фона
        createButton.getElement().getStyle().set("color", "#ffffff"); // Белый цвет текста
        createButton.getElement().getStyle().set("border", "none"); // Без границы
        createButton.getElement().getStyle().set("width", "100%"); // Ширина кнопки равна ширине родительского элемента
        createButton.getElement().getStyle().set("border-radius", "5px"); // Округлые края кнопки
        createButton.getElement().getStyle().set("padding", "10px"); // Размер кнопки

        VerticalLayout dialogLayout = new VerticalLayout(userIdField, cipherComboBox, cipherModeComboBox, paddingComboBox, createButton);
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(true);
        createChatDialog.add(dialogLayout);

        createButton.addClickListener(e -> {
            long generatedId = 0;
            try {
                long userId = Long.parseLong(userIdField.getValue());

                if (!userService.checkIfExists(userId)) {
                    throw new UserNotFoundException("Please, enter existing User ID");
                }

                if (userId == user.getPersonId()) {
                    throw new DuplicateDialogueException("You cannot create a dialogue with yourself!");
                }

                if (currentCompanions.contains(userService.getUserByPersonId(userId).get().getUsername())) {
                    throw new DuplicateDialogueException("Dialogue already exists with this user!");
                }


                String cipher = cipherComboBox.getValue();
                String cipherMode = cipherModeComboBox.getValue();
                String padding = paddingComboBox.getValue();

                if (cipher != null && cipherMode != null && padding != null) {
                    generatedId = backend.createRoom(user.getPersonId(), userId, cipher, cipherMode, padding);
                    currentCompanions.add(userService.getUserByPersonId(userId).get().getUsername());
                    Backend.launchNewTopic(String.format("messageStorageOfChat-%s", generatedId), 1);
                    log.info("######################" + "Room with id: '" + generatedId + "' was created!" + "######################");

                    cipherComboBox.clear();
                    paddingComboBox.clear();
                    cipherModeComboBox.clear();
                    userIdField.clear();
                    createChatDialog.close();
                } else {
                    Notification.show("Please fill in all fields");
                }
            } catch (DuplicateDialogueException | UserNotFoundException exc) {
                Notification.show(exc.getMessage());
            } catch (NumberFormatException exc) {
                Notification.show("Please, enter a valid numeric User ID!");
            }

           /* boolean a = backend.setConnectionWithRoom(generatedId);
            log.info("########" + a + "##########");*/

            //Теперь можно кинуть в список чатов новый чат и обновить его отображение
            Broadcaster.broadcast("updateGrid");
           /* updatedDialogues.add(backend.getDialoguesForOwner(generatedId));
            chatList.setItems(updatedDialogues);
            UI.getCurrent().navigate(UI.getCurrent().getInternals().getActiveViewLocation().getPath());*/

        });
    }

    private void openCreateChatDialog() {
        createChatDialog.open();
    }

    private void applyStyles() {
        // Стили для основной формы
        getElement().getStyle().set("background", "linear-gradient(to right, #3a1c71, #d76d77, #ffaf7b)");
        getElement().getStyle().set("padding", "20px");
        // getElement().getStyle().set("border-radius", "15px"); // Округлые края всей формы
        getElement().getStyle().set("box-shadow", "0 2px 10px rgba(0, 0, 0, 0.1)");

        // Стили для списка чатов
        chatList.getElement().getStyle().set("border-radius", "15px"); // Округлые края списка чатов
        chatList.getElement().getStyle().set("background-color", "#ffffff");
        chatList.getElement().getStyle().set("border", "1px solid #e0e0e0");
        chatList.getElement().getStyle().set("padding", "10px");

        // Стили для окна чата
        chatWindow.getElement().getStyle().set("background-color", "#ffffff");
        chatWindow.getElement().getStyle().set("border-radius", "15px"); // Округлые края
        chatWindow.getElement().getStyle().set("border", "1px solid #e0e0e0");
        chatWindow.getElement().getStyle().set("padding", "10px");

        // Стили для текстовой области сообщений
        messagesLayout.getElement().getStyle().set("padding", "10px");
        messagesLayout.getElement().getStyle().set("background-color", "#f9f9f9");

        // Стили для поля ввода сообщения
        messageInput.getElement().getStyle().set("padding", "10px");
        messageInput.getElement().getStyle().set("font-size", "14px");
        messageInput.getElement().getStyle().set("color", "#333333");
        messageInput.getElement().getStyle().set("background-color", "#ffffff");
        messageInput.getElement().getStyle().set("border", "1px solid #e0e0e0");
        messageInput.getElement().getStyle().set("border-radius", "20px"); // Округленные края

        // Стили для кнопок
        uploadButton.getElement().getStyle().set("background-color", "#ffffff");
        uploadButton.getElement().getStyle().set("border", "1px solid #e0e0e0");
        uploadButton.getElement().getStyle().set("border-radius", "5px");
        uploadButton.getElement().getStyle().set("color", "#007bff");
        uploadButton.getElement().getStyle().set("padding", "5px");

        sendButton.getElement().getStyle().set("background-color", "#007bff");
        sendButton.getElement().getStyle().set("border", "none");
        sendButton.getElement().getStyle().set("color", "#ffffff");
        sendButton.getElement().getStyle().set("border-radius", "5px");
        sendButton.getElement().getStyle().set("padding", "5px 10px"); // Размер кнопки уменьшен

        // Выравнивание текста в кнопках
        uploadButton.getElement().getStyle().set("text-align", "center");
        sendButton.getElement().getStyle().set("text-align", "center");

        // Стили для сообщения
        chatWindow.getElement().getStyle().set("overflow", "auto"); // Скролл если контента больше, чем размер окна

        // Добавление стилей для заголовка (логотип, текст и кнопка выхода)
        headerLayout.getElement().getStyle().set("border-bottom", "1px solid #e0e0e0");
        headerLayout.getElement().getStyle().set("padding", "10px 15px");
        headerLayout.getElement().getStyle().set("background-color", "#f5f5f5");

        // Отступ между списком чатов и формой сообщений
        chatListLayout.getElement().getStyle().set("margin-right", "15px");  // Увеличенный отступ
    }

    public class UserNotFoundException extends Exception {
        public UserNotFoundException(String message) {
            super(message);
        }
    }

    public class DuplicateDialogueException extends Exception {
        public DuplicateDialogueException(String message) {
            super(message);
        }
    }

    @PreDestroy
    private void preDestroyer() {
        if (kafkaConsumer != null) {
            messageThreadExecutor.shutdown();
            if (messageThreadExecutor.isShutdown()) {
                kafkaConsumer.close();
            }
        }

        if (room != null) {
            myPrivateKey = null;
            myPublicNumber = null;
            companionsPublicNumber = null;
            sharedSecret = null;
            backend.cutConnectionWithRoom(room.getRoomId());
            room = null;
        }
        user = null;
    }

    private void cleanup() {
        if (room != null) {
            myPrivateKey = null;
            myPublicNumber = null;
            companionsPublicNumber = null;
            sharedSecret = null;
            backend.cutConnectionWithRoom(room.getRoomId());
            room = null;
        }
        user = null;
    }

    private class Backend {

        private long createRoom(long ownerId, long guestId, String cipher, String cipherMode, String padding) {
            return server.createRoom(ownerId, guestId, cipher, cipherMode, padding);
        }

        private boolean setConnectionWithRoom(long roomId) {
            return server.connectRoom(user.getPersonId(), roomId);
        }

        private boolean cutConnectionWithRoom(long roomId) {
            return server.disconnectRoom(user.getPersonId(), roomId);
        }

        private boolean executeRoom(long roomId) {
            return server.deleteRoom(roomId);
        }

        private List<String> updateDialoguesBeforeEntering() {
            long[] connectedRooms = user.getConnectedRooms();

            List<String> companionsNames = new ArrayList<>();
            long companionId = 0;

            for (int i = 0; i < connectedRooms.length; i++) {
                Optional<Room> getRoom = roomService.getRoomByRoomId(connectedRooms[i]);

                if (getRoom.isPresent()) {
                    Room exactRoom = getRoom.get();

                    if (exactRoom.getOwnerId() == user.getPersonId()) {
                        companionId = exactRoom.getGuestId();
                    } else if (exactRoom.getGuestId() == user.getPersonId()) {
                        companionId = exactRoom.getOwnerId();
                    }

                    Optional<User> getUser = userService.getUserByPersonId(companionId);
                    if (getUser.isPresent()) {
                        companionsNames.add(getUser.get().getUsername());
                    }
                }
            }

            return companionsNames;
        }

        private String getDialoguesForOwner(long roomId) {
            String companionUsername = "";
            Optional<Room> getRoom = roomService.getRoomById(roomId);
            if (getRoom.isPresent()) {
                long guestId = getRoom.get().getGuestId();
                Optional<User> getUser = userService.getUserByPersonId(guestId);
                if (getUser.isPresent()) {
                    companionUsername = getUser.get().getUsername();
                }
            }
            return companionUsername;
        }

        private Room getRoomByUsername(String companionUsername, long loggedInUserId) {
            Optional<User> getUser = userService.findUserByUsername(companionUsername);
            if (getUser.isPresent()) {
                User user = getUser.get();
                long[] connectedRooms = user.getConnectedRooms();

                for (int i = 0; i < connectedRooms.length; i++) {
                    Optional<Room> getRoom = roomService.getRoomByRoomId(connectedRooms[i]);

                    if (getRoom.isPresent()) {
                        Room exactRoom = getRoom.get();
                        if (exactRoom.getOwnerId() == loggedInUserId || exactRoom.getGuestId() == loggedInUserId) {
                            return exactRoom;
                        }
                    }
                }
            }
            return null;
        }

        private SerializedMessage serializeMessage(KafkaMessage kafkaMessage) {
            return SerializedMessage.builder()
                    .content(kafkaMessage.getContent())
                    .id(kafkaMessage.getId())
                    .messageIdentity(kafkaMessage.getMessageIdentity())
                    .timestamp(kafkaMessage.getTimestamp())
                    .roomId(kafkaMessage.getRoomId())
                    .senderId(kafkaMessage.getSenderId())
                    .recipientId(kafkaMessage.getRecipientId())
                    .build();
        }

        private KafkaMessage deserializeMessage(SerializedMessage serializedMessage) {
            return KafkaMessage.builder()
                    .content(serializedMessage.getContent())
                    .messageIdentity(serializedMessage.getMessageIdentity())
                    .timestamp(serializedMessage.getTimestamp())
                    .senderId(serializedMessage.getSenderId())
                    .recipientId(serializedMessage.getRecipientId())
                    .roomId(serializedMessage.getRoomId())
                    .id(serializedMessage.getId()).build();
        }

        private byte[] handleInputStream(InputStream dataStream) throws IOException {
            //временный буфер для записи байтов
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            //массив для временного хранения данных при чтении из dataStream
            byte[] storage = new byte[1024];

            //храним количество байт, фактически считанных за одну итерацию
            int readByIteration;

            //читаем данные из стрима кусками до 1024 байт до тех пор, пока не достигнут конец потока (readByIteration != -1)
            while ((readByIteration = dataStream.read(storage, 0, storage.length)) != -1) {
                //пишем прочитанные байты в буфер, начиная с первого байта до количества readByIteration.
                buffer.write(storage, 0, readByIteration);
            }

            //преобразуем накопленные в буфере данные в массив байтов и возвращаем его
            return buffer.toByteArray();
        }

        public static void launchNewTopic(String name, int partitions) {
            Properties config = new Properties();
            config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9093");

            try (AdminClient kafkaAdministrator = AdminClient.create(config)) {

                if (!kafkaAdministrator.listTopics().names().get().contains(name)) {
                    NewTopic topic = new NewTopic(name, partitions, (short) 1);
                    kafkaAdministrator.createTopics(Collections.singletonList(topic)).all().get();

                    log.info("Topic '" + name + "' was successfully created!");
                } else {
                    log.info("Topic with name '" + name + "' already exists!");
                }
            } catch (Exception exc) {
                log.error(exc.getMessage(), exc);
            }
        }

        private PaddingService initializePadding() {
            PaddingService padding;
            switch (room.getPadding()) {
                case "ISO10126":
                    padding = new ISO10126();
                    break;
                case "PKCS7":
                    padding = new PKCS7();
                    break;
                case "ANSIx923":
                    padding = new ANSIx923();
                    break;
                case "Zeros":
                    padding = new Zeros();
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported padding!");
            }
            return padding;
        }

        private ICipher initializeCipher(byte[] key) {
            ICipher cipher;
            if (room.getCipher().equals("RC5")) {
                cipher = new RC5(room.getBlockSizeInBits(), 12, key, key.length);
            } else {
                cipher = new Twofish(128, key);
            }
            return cipher;
        }

        private IModeCipher initializeMode() {
            IModeCipher mode;
            switch (room.getCipherMode()) {
                case "CBC":
                    mode = new CBC();
                    break;
                case "Random Delta":
                    mode = new RandomDelta();
                    break;
                case "CFB":
                    mode = new CFB();
                    break;
                case "ECB":
                    mode = new ECB();
                    break;
                case "OFB":
                    mode = new OFB();
                    break;
                case "PCBC":
                    mode = new PCBC();
                    break;
                case "CTR":
                    mode = new CTR();
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported cipher mode!");
            }
            return mode;
        }

        private static final Random RANDOM = new Random();

        //Преобразование массива байт в BigInteger
        private static BigInteger bytesToBigInteger(byte[] bytes) {
            return new BigInteger(1, bytes); // Используем конструктор BigInteger с флагом 1 для положительного значения
        }

        //Преобразование BigInteger в массив байт
        private static byte[] bigIntegerToBytes(BigInteger bigInteger) {
            return bigInteger.toByteArray();
        }

        //Генерация приватного ключа
        public static byte[] generatePrivateKey(byte[] pBytes) {
            BigInteger p = bytesToBigInteger(pBytes);
            BigInteger privateKey;
            do {
                privateKey = new BigInteger(p.bitLength(), RANDOM); // Генерируем число с тем же количеством бит, что и p
            } while (privateKey.compareTo(BigInteger.ONE) <= 0 || privateKey.compareTo(p) >= 0); // Убеждаемся, что 1 < privateKey < p
            return bigIntegerToBytes(privateKey);
        }

        //Генерация публичного ключа
        public static byte[] generatePublicKey(byte[] pBytes, byte[] gBytes, byte[] privateKeyBytes) {
            BigInteger p = bytesToBigInteger(pBytes);
            BigInteger g = bytesToBigInteger(gBytes);
            BigInteger privateKey = bytesToBigInteger(privateKeyBytes);
            // Вычисляем публичное число: g^privateKey mod p
            BigInteger publicKey = g.modPow(privateKey, p);
            return bigIntegerToBytes(publicKey);
        }

        //Вычисление общего секрета
        public static byte[] computeSharedSecret(byte[] publicKeyOtherUserBytes, byte[] privateKeyBytes, byte[] pBytes) {
            BigInteger publicKeyOtherUser = bytesToBigInteger(publicKeyOtherUserBytes);
            BigInteger privateKey = bytesToBigInteger(privateKeyBytes);
            BigInteger p = bytesToBigInteger(pBytes);
            // Вычисляем общий секрет: publicKeyOtherUser^privateKey mod p
            BigInteger sharedSecret = publicKeyOtherUser.modPow(privateKey, p);
            return bigIntegerToBytes(sharedSecret);
        }

        //генерация хэша на основе roomId
        public static byte[] generateHash(long roomId) throws Exception {
            //используем SHA-256 для получения хэша из roomId
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(String.valueOf(roomId).getBytes(StandardCharsets.UTF_8));
        }

        //функция для применения хэша к байтовому массиву (шифрование/дешифрование)
        public static byte[] applyHash(byte[] data, byte[] hash) {
            byte[] result = new byte[data.length];
            for (int i = 0; i < data.length; i++) {
                //применяем XOR побайтно
                result[i] = (byte) (data[i] ^ hash[i % hash.length]);
            }
            return result;
        }

    }
}