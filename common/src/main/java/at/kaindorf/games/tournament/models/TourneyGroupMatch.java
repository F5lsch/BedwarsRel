package at.kaindorf.games.tournament.models;

import at.kaindorf.games.tournament.Tournament;

import java.util.List;

public class TourneyGroupMatch extends TourneyMatch {

  public TourneyGroupMatch(List<TourneyTeam> teams) {
    super(teams);
  }



  public TourneyTeamStatistics getMatchStatistics(TourneyTeam team) {
    return team.getStatistics().stream().filter(stat -> stat.getMatch().equals(this)).findFirst().orElse(null);
  }

  public TourneyGroup getGroup() {
    if (teams.size() > 0) {
      return Tournament.getInstance().getGroups().stream().filter(g -> g.getTeams().stream().anyMatch(t -> t.getName().equals(teams.get(0).getName()))).findFirst().orElse(new TourneyGroup("Undefined"));
    }
    return new TourneyGroup("Undefined");
  }
}
