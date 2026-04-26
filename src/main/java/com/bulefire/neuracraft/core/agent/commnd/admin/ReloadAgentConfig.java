package com.bulefire.neuracraft.core.agent.commnd.admin;

import com.bulefire.neuracraft.compatibility.command.FullCommand;
import com.bulefire.neuracraft.core.agent.AgentController;
import com.bulefire.neuracraft.core.util.NoAgentFound;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ReloadAgentConfig extends FullCommand.AbsCommand {
    @Override
    public int run(@NotNull CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
        var agentManager = AgentController.getInstance().getAgentManager();
        String agentName;
        UUID agentUUID;
        try {
            agentName = StringArgumentType.getString(commandContext, "agentName");
        } catch (IllegalArgumentException e) {
            agentName = null;
        }
        try {
            agentUUID = UUID.fromString(StringArgumentType.getString(commandContext, "agentUUID"));
        } catch (IllegalArgumentException e) {
            agentUUID = null;
        }
        if (agentName != null && agentUUID == null) {
            // use name
            try {
                agentManager.reloadAgentConfig(agentName);
            } catch (NoAgentFound e) {
                feedback(commandContext.getSource(), Component.translatable("neuracraft.agent.command.delete.noAgent", agentUUID));
            }
        } else if (agentName == null && agentUUID != null) {
            // use uuid
            try {
                agentManager.reloadAgentConfig(agentUUID);
            } catch (NoAgentFound e) {
                feedback(commandContext.getSource(), Component.translatable("neuracraft.agent.command.delete.noAgent", agentUUID));
            }
        } else if (agentName == null && agentUUID == null) {
            // reload all
            agentManager.reloadAllAgentConfig();
        } else {
            return 0;
        }

        return 1;
    }
}
