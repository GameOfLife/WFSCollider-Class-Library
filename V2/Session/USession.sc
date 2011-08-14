USession{

    /*
    *   tracks -> Array[UChain]
    *   scores -> Array[UEvent]
    */
    var <>tracks, <>scores;

    *new{ |tracks, scores|
        ^super.newCopyArgs(tracks, scores)
    }

    startAll {

    }

    /*
    *   server: Server or Array[Server]
    */
    startTracks { |server|
        var chains = tracks.collect{ |x| x <| U(\output) };
        fork{
            chains.do( _.prepare(server) );
            server.sync;
            chains.do( _.start(server) );
        };
        ^chains
    }

}