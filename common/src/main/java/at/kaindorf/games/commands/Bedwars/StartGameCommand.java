package at.kaindorf.games.commands.Bedwars;

import at.kaindorf.games.commands.BaseCommand;
import at.kaindorf.games.commands.ICommand;
import com.google.common.collect.ImmutableMap;
import at.kaindorf.games.BedwarsRel;
import at.kaindorf.games.game.Game;
import at.kaindorf.games.utils.ChatWriter;
import java.util.ArrayList;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class StartGameCommand extends BaseCommand implements ICommand {

  public StartGameCommand(BedwarsRel plugin) {
    super(plugin);
  }

  @Override
  public boolean execute(CommandSender sender, ArrayList<String> args) {
    if (!sender.hasPermission("bw." + this.getPermission())) {
      return false;
    }

    Game game = this.getPlugin().getGameManager().getGame(args.get(0));
    if (game == null) {
      sender.sendMessage(ChatWriter.pluginMessage(ChatColor.RED
          + BedwarsRel
          ._l(sender, "errors.gamenotfound", ImmutableMap.of("game", args.get(0).toString()))));
      return false;
    }

    game.run(sender);
    return true;
  }

  @Override
  public String[] getArguments() {
    return new String[]{"game"};
  }

  @Override
  public String getCommand() {
    return "start";
  }

  @Override
  public String getDescription() {
    return BedwarsRel._l("commands.start.desc");
  }

  @Override
  public String getName() {
    return BedwarsRel._l("commands.start.name");
  }

  @Override
  public String getPermission() {
    return "setup";
  }

}
