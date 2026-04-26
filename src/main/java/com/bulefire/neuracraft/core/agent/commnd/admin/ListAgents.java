package com.bulefire.neuracraft.core.agent.commnd.admin;

import com.bulefire.neuracraft.compatibility.command.FullCommand;
import com.bulefire.neuracraft.core.agent.Agent;
import com.bulefire.neuracraft.core.agent.AgentController;
import static com.bulefire.neuracraft.core.command.util.ComponentGenerator.withHoverAndCopy;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.*;
import org.jetbrains.annotations.NotNull;

public class ListAgents extends FullCommand.AbsCommand {
    @Override
    public int run(@NotNull CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
        MutableComponent component = Component.literal("");
        for (Agent agent : AgentController.getInstance().getAgentManager().getAllAgents()) {
            var join = Component.literal("join in")
                                .withStyle(style -> style
                                        .withColor(TextColor.parseColor("#FF69B4"))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("neuracraft.agent.command.list.chatroom.hover.join", agent.getUUID().toString())))
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/neuracraft agent join " + agent.getUUID()))
                                );
            component.append(Component.translatable(
                    "neuracraft.agent.command.list.agent.single",
                    withHoverAndCopy(agent.getName(), "#7CFC00"),
                    withHoverAndCopy(agent.getUUID().toString(), "#00ffff"),
                    join
            ));
        }
        feedback(commandContext.getSource(), Component.translatable("neuracraft.agent.command.list.agent", component));
        return 1;
    }
}
