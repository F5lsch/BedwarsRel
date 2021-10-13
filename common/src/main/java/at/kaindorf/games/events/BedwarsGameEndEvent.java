package at.kaindorf.games.events;

import at.kaindorf.games.game.Game;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BedwarsGameEndEvent extends Event {

  private static final HandlerList handlers = new HandlerList();
  private Game game = null;

  public BedwarsGameEndEvent(Game game) {
    this.game = game;
  }

  public static HandlerList getHandlerList() {
    return BedwarsGameEndEvent.handlers;
  }

  public Game getGame() {
    return this.game;
  }

  @Override
  public HandlerList getHandlers() {
    return BedwarsGameEndEvent.handlers;
  }

}
