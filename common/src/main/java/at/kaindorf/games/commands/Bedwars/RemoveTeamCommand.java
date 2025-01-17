package at.kaindorf.games.commands.Bedwars;

import at.kaindorf.games.BedwarsRel;
import at.kaindorf.games.commands.BaseCommand;
import at.kaindorf.games.game.Game;
import at.kaindorf.games.game.GameState;
import at.kaindorf.games.game.Team;
import at.kaindorf.games.utils.ChatWriter;
import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class RemoveTeamCommand extends BaseCommand {

  public RemoveTeamCommand(BedwarsRel plugin) {
    super(plugin);
  }

  @Override
  public boolean execute(CommandSender sender, ArrayList<String> args) {
    if (!sender.hasPermission("bw." + this.getPermission())) {
      return false;
    }

    Game game = this.getPlugin().getGameManager().getGame(args.get(0));
    String name = args.get(1);

    if (game == null) {
      sender.sendMessage(ChatWriter.pluginMessage(ChatColor.RED
          + BedwarsRel
          ._l(sender, "errors.gamenotfound", ImmutableMap.of("game", args.get(0).toString()))));
      return false;
    }

    if (game.getState() != GameState.STOPPED) {
      sender.sendMessage(
          ChatWriter.pluginMessage(ChatColor.RED + BedwarsRel
              ._l(sender, "errors.notwhilegamerunning")));
      return false;
    }

    Team theTeam = game.getTeam(name);
    if (theTeam == null) {
      sender.sendMessage(
          ChatWriter.pluginMessage(ChatColor.RED + BedwarsRel._l(sender, "errors.teamnotfound")));
      return false;
    }

    game.removeTeam(theTeam);
    sender.sendMessage(
        ChatWriter.pluginMessage(ChatColor.GREEN + BedwarsRel._l(sender, "success.teamremoved")));
    return true;
  }

  @Override
  public String[] getArguments() {
    return new String[]{"game", "name"};
  }

  @Override
  public String getCommand() {
    return "removeteam";
  }

  @Override
  public String getDescription() {
    return BedwarsRel._l("commands.removeteam.desc");
  }

  @Override
  public String getName() {
    return BedwarsRel._l("commands.removeteam.name");
  }

  @Override
  public String getPermission() {
    return "setup";
  }

}
