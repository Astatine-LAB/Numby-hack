package cqb13.NumbyHack.modules.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.commands.SharedSuggestionProvider;

public class ClearChat extends Command {
    public ClearChat() {
        super("clear-chat", "Clears your chat.", "clear", "cls");
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.executes(context -> {
            mc.gui.getChat().clearMessages(false);
            return SINGLE_SUCCESS;
        });
    }
}
