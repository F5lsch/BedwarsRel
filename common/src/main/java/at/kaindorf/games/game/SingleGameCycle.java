package at.kaindorf.games.game;

import at.kaindorf.games.communication.dto.Leaderboard;
import at.kaindorf.games.events.BedwarsGameEndEvent;
import at.kaindorf.games.tournament.Tournament;
import at.kaindorf.games.tournament.models.*;
import at.kaindorf.games.utils.ChatWriter;
import at.kaindorf.games.utils.TournamentLogger;
import at.kaindorf.games.utils.Utils;
import com.google.common.collect.ImmutableMap;
import at.kaindorf.games.BedwarsRel;
import at.kaindorf.games.statistics.PlayerStatistic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class SingleGameCycle extends GameCycle {

  public SingleGameCycle(Game game) {
    super(game);
  }

  private void kickPlayer(Player player, boolean wasSpectator) {
    for (Player freePlayer : this.getGame().getFreePlayers()) {
      player.showPlayer(freePlayer);
    }

    if (wasSpectator && this.getGame().isFull()) {
      this.getGame().playerLeave(player, false);
      return;
    }

    if (BedwarsRel.getInstance().toMainLobby()) {
      if (BedwarsRel.getInstance().allPlayersBackToMainLobby()) {
        this.getGame().playerLeave(player, false);
        return;
      } else {
        player.teleport(this.getGame().getLobby());
      }
    } else {
      player.teleport(this.getGame().getLobby());
    }

    if (BedwarsRel.getInstance().isHologramsEnabled()
        && BedwarsRel.getInstance().getHolographicInteractor() != null
        && this.getGame().getLobby() == player.getWorld()) {
      BedwarsRel.getInstance().getHolographicInteractor().updateHolograms(player);
    }

    if (BedwarsRel.getInstance().statisticsEnabled()) {
      PlayerStatistic statistic =
          BedwarsRel.getInstance().getPlayerStatisticManager().getStatistic(player);
      BedwarsRel.getInstance().getPlayerStatisticManager().storeStatistic(statistic);

      if (BedwarsRel.getInstance().getBooleanConfig("statistics.show-on-game-end", true)) {
        BedwarsRel.getInstance().getServer().dispatchCommand(player, "bw stats");
      }
    }

    this.getGame().setPlayerDamager(player, null);

    PlayerStorage storage = this.getGame().getPlayerStorage(player);
    storage.clean();
    storage.loadLobbyInventory(this.getGame());
  }

  @Override
  public void onGameEnds() {
    // Reset scoreboard first
    this.getGame().resetScoreboard();

    // First team players, they get a reserved slot in lobby
    for (Player p : this.getGame().getTeamPlayers()) {
      this.kickPlayer(p, false);
    }

    // and now the spectators
    List<Player> freePlayers = new ArrayList<Player>(this.getGame().getFreePlayers());
    for (Player p : freePlayers) {
      this.kickPlayer(p, true);
    }

    // reset countdown prevention breaks
    this.setEndGameRunning(false);

    // Reset team chests
    for (Team team : this.getGame().getTeams().values()) {
      team.setInventory(null);
      team.getChests().clear();
    }

    // clear protections
    this.getGame().clearProtections();

    // reset region
    this.getGame().resetRegion();

    // Restart lobby directly?
    if (this.getGame().isStartable() && this.getGame().getLobbyCountdown() == null) {
      GameLobbyCountdown lobbyCountdown = new GameLobbyCountdown(this.getGame());
      lobbyCountdown.runTaskTimer(BedwarsRel.getInstance(), 20L, 20L);
      this.getGame().setLobbyCountdown(lobbyCountdown);
    }

    // set state and with that, the sign
    this.getGame().setState(GameState.WAITING);
    this.getGame().updateScoreboard();

    /*check if game was in Tournament*/
    if (this.getGame().getMatch() != null) {
      TourneyMatch match = this.getGame().getMatch();
      this.getGame().setMatch(null);

      // move match to done
      if (!match.isAborted()) {
        if (match instanceof TourneyGroupMatch) {
          Tournament.getInstance().getGroupStage().matchPlayed((TourneyGroupMatch) match);
        } else if (match instanceof TourneyKoMatch) {
          Tournament.getInstance().getKoStage().currentKoRound().matchPlayed((TourneyKoMatch) match);
        }
      }

      // set teams to be ready
      match.getTeams().forEach(t -> t.setGame(null));
      match.setAborted(false);
    }
    this.getGame().setDisableLeave(false);
  }

  @Override
  public void onGameLoaded() {
    // Reset on game end
  }

  @Override
  public void onGameOver(GameOverTask task) {
    // set winning team of the round
    TourneyMatch match = task.getCycle().getGame().getMatch();
    if (match != null && task.getWinner() != null && task.getCounter() == task.getStartCount()) {
      Optional<TourneyTeam> winner = Tournament.getInstance().getTourneyTeamOfPlayer(task.getWinner().getPlayers().get(0));
      if (winner.isPresent()) {
        Optional<TourneyGameStatistic> statistics = winner.get().getStatistics().stream().filter(st -> st.getMatch() == match).findFirst();
        statistics.ifPresent(TourneyGameStatistic::setWin);
        TournamentLogger.info().logMatchWin(task.getWinner().getDisplayName());
      }
    }

    if (task.getCounter() == task.getStartCount() && task.getWinner() != null) {
      for (Player aPlayer : this.getGame().getPlayers()) {
        if (aPlayer.isOnline()) {
          aPlayer.sendMessage(
              ChatWriter.pluginMessage(ChatColor.GOLD + BedwarsRel._l(aPlayer, "ingame.teamwon",
                  ImmutableMap.of("team", task.getWinner().getDisplayName() + ChatColor.GOLD))));
        }
      }
      this.getGame().stopWorkers();
    } else if (task.getCounter() == task.getStartCount() && task.getWinner() == null) {
      for (Player aPlayer : this.getGame().getPlayers()) {
        if (aPlayer.isOnline()) {
          aPlayer.sendMessage(
              ChatWriter.pluginMessage(ChatColor.GOLD + BedwarsRel._l(aPlayer, "ingame.draw")));
        }
      }
    }

    if (this.getGame().getPlayers().size() == 0 || task.getCounter() == 0) {
      BedwarsGameEndEvent endEvent = new BedwarsGameEndEvent(this.getGame());
      BedwarsRel.getInstance().getServer().getPluginManager().callEvent(endEvent);

      this.onGameEnds();
      task.cancel();
    } else {
      for (Player aPlayer : this.getGame().getPlayers()) {
        if (aPlayer.isOnline()) {
          aPlayer.sendMessage(
              ChatWriter.pluginMessage(
                  ChatColor.AQUA + BedwarsRel
                      ._l(aPlayer, "ingame.backtolobby", ImmutableMap.of("sec",
                          ChatColor.YELLOW.toString() + task.getCounter() + ChatColor.AQUA))));
        }
      }
    }

    task.decCounter();
  }

  @Override
  public void onGameStart() {
    // Reset on game end
  }

  @Override
  public boolean onPlayerJoins(Player player) {
    if (this.getGame().isFull() && !player.hasPermission("bw.vip.joinfull")) {
      if (this.getGame().getState() != GameState.RUNNING
          || !BedwarsRel.getInstance().spectationEnabled()) {
        player.sendMessage(
            ChatWriter.pluginMessage(ChatColor.RED + BedwarsRel._l(player, "lobby.gamefull")));
        return false;
      }
    } else if (this.getGame().isFull() && player.hasPermission("bw.vip.joinfull")) {
      if (this.getGame().getState() == GameState.WAITING) {
        List<Player> players = this.getGame().getNonVipPlayers();

        if (players.size() == 0) {
          player.sendMessage(
              ChatWriter
                  .pluginMessage(ChatColor.RED + BedwarsRel._l(player, "lobby.gamefullpremium")));
          return false;
        }

        Player kickPlayer = null;
        if (players.size() == 1) {
          kickPlayer = players.get(0);
        } else {
          kickPlayer = players.get(Utils.randInt(0, players.size() - 1));
        }

        kickPlayer
            .sendMessage(
                ChatWriter.pluginMessage(ChatColor.RED + BedwarsRel
                    ._l(kickPlayer, "lobby.kickedbyvip")));
        this.getGame().playerLeave(kickPlayer, false);
      } else {
        if (this.getGame().getState() == GameState.RUNNING
            && !BedwarsRel.getInstance().spectationEnabled()) {
          player.sendMessage(
              ChatWriter
                  .pluginMessage(ChatColor.RED + BedwarsRel._l(player, "errors.cantjoingame")));
          return false;
        }
      }
    }

    if(BedwarsRel.getInstance().isLeaderBoardActive()) {
      Leaderboard.getInstance().newJoinedGame(String.valueOf(player.getUniqueId()), true);
    }

    return true;
  }

  @Override
  public void onPlayerLeave(Player player) {
    // teleport to join location
    PlayerStorage storage = this.getGame().getPlayerStorage(player);

    if (BedwarsRel.getInstance().toMainLobby()) {
      if (BedwarsRel.getInstance().isHologramsEnabled()
          && BedwarsRel.getInstance().getHolographicInteractor() != null
          && this.getGame().getMainLobby().getWorld() == player.getWorld()) {
        BedwarsRel.getInstance().getHolographicInteractor().updateHolograms(player);
      }

      player.teleport(this.getGame().getMainLobby());
    } else {
      if (BedwarsRel.getInstance().isHologramsEnabled()
          && BedwarsRel.getInstance().getHolographicInteractor() != null
          && storage.getLeft() == player.getWorld()) {
        BedwarsRel.getInstance().getHolographicInteractor().updateHolograms(player);
      }

      player.teleport(storage.getLeft());
    }

    if (this.getGame().getState() == GameState.RUNNING && !this.getGame().isStopping()
        && !this.getGame().isSpectator(player)) {
      this.checkGameOver();
    }

    if(BedwarsRel.getInstance().isLeaderBoardActive()) {
      Leaderboard.getInstance().newJoinedGame(String.valueOf(player.getUniqueId()), false);
    }
  }

}
