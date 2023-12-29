package at.kaindorf.games.commands.Bedwars;

import at.kaindorf.games.BedwarsRel;
import at.kaindorf.games.commands.BaseCommand;
import at.kaindorf.games.commands.ICommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;

public class ClearLeaderboardCommand extends BaseCommand implements ICommand {

    public ClearLeaderboardCommand(BedwarsRel plugin) {
        super(plugin);
    }

    @Override
    public boolean execute(CommandSender sender, ArrayList<String> args) {
        if (!sender.hasPermission("bw." + this.getPermission())) {
            return false;
        }

        BedwarsRel.getInstance().setActiveLeaderboard(null);
        sender.sendMessage(ChatColor.GREEN + "Leaderboard has been cleared!!!");
        return true;
    }

    @Override
    public String[] getArguments() {
        return new String[0];
    }

    @Override
    public String getCommand() {
        return "clear-leaderboard";
    }

    @Override
    public String getDescription() {
        return "clear leaderboard";
    }

    @Override
    public String getName() {
        return "clear-leaderboard";
    }

    @Override
    public String getPermission() {
        return "setup";
    }
}
