package com.project.Messenger.UI.views.PageUpdater;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.shared.Registration;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
public class Broadcaster {

    private static final Executor broadcasterExecutor = Executors.newSingleThreadExecutor();
    private static final Set<BiConsumer<UI, String>> listeners = new CopyOnWriteArraySet<>();

    public static synchronized void register(BiConsumer<UI, String> listener, UI ui) {
        listeners.add((uiObj, message) -> listener.accept(ui, message));
    }

    public static synchronized void unregister(BiConsumer<UI, String> listener) {
        listeners.remove(listener);
    }

    public static synchronized void broadcast(String message) {
        for (BiConsumer<UI, String> listener : listeners) {
            broadcasterExecutor.execute(() -> listener.accept(UI.getCurrent(), message));
        }
    }
}