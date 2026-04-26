package com.bulefire.neuracraft.core.agent.commnd;

import com.bulefire.neuracraft.core.agent.AgentController;
import com.bulefire.neuracraft.core.agent.commnd.admin.ListAgents;
import com.bulefire.neuracraft.core.agent.commnd.admin.ReloadAgentConfig;
import com.bulefire.neuracraft.core.agent.commnd.control.Create;
import com.bulefire.neuracraft.core.agent.commnd.control.Delete;
import com.bulefire.neuracraft.core.agent.commnd.self.Exit;
import com.bulefire.neuracraft.core.agent.commnd.self.Find;
import com.bulefire.neuracraft.core.agent.commnd.self.Join;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class NCCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> getCommands() {
        return AgentController.getInstance().getGAME_COMMAND().getAgentBaseCommand()
                              .requires(source -> source.hasPermission(0))
                              .executes(new AgentCommand())
                              .then(Commands.literal("create")
                                            .then(Commands.argument("agentName", StringArgumentType.word())
                                                          .then(Commands.argument("chatModel", StringArgumentType.word())
                                                                        .executes(new Create(true))
                                                                        .then(Commands.literal("withoutGreet")
                                                                                      .executes(new Create(false))
                                                                        )
                                                          )
                                            )
                              )
                              
                              .then(Commands.literal("delete")
                                            .executes(new Delete())
                                            .then(Commands.argument("agentName", StringArgumentType.word())
                                                          .executes(new Delete())
                                            )
                                            .then(Commands.argument("agentUUID", StringArgumentType.string())
                                                          .executes(new Delete())
                                            )
                              )
                              
                              .then(Commands.literal("join")
                                            .then(Commands.argument("agentName", StringArgumentType.word())
                                                          .executes(new Join())
                                            )
                                            .then(Commands.argument("agentUUID", StringArgumentType.string())
                                                          .executes(new Join())
                                            )
                              )
                              
                              .then(Commands.literal("exit")
                                            .executes(new Exit())
                              )
                              
                              .then(Commands.literal("show")
                                            .executes(new Show())
                              )
                              
                              .then(Commands.literal("find")
                                            .executes(new Find())
                              )
                              
                              //                .then(Commands.literal("invite")
                              //                        .then(Commands.argument("inviteName", StringArgumentType.word())
                              //                                .executes(new Invite())
                              //                        )
                              //                )
                              //
                              //                .then(Commands.literal("kick")
                              //                        .then(Commands.argument("kickName", StringArgumentType.word())
                              //                                .executes(new Kick())
                              //                        )
                              //                )
                              //
                              .then(Commands.literal("list")
                                            .requires(source -> source.hasPermission(2))
                                            .executes(new ListAgents())
                              )
                              
                              .then(Commands.literal("reload")
                                            .requires(source -> source.hasPermission(2))
                                            .executes(new ReloadAgentConfig())
                                            .then(Commands.argument("agentName", StringArgumentType.word())
                                                          .executes(new ReloadAgentConfig())
                                            )
                                            .then(Commands.argument("agentUUID", StringArgumentType.string())
                                                          .executes(new ReloadAgentConfig())
                                            )
                              );
    }

    public static void buildCommands() {
        getCommands();
    }
}
