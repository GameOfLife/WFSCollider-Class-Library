USession{

	classvar <>all, <>current;
	
    /*
    *   tracks -> Array[UChain]
    *   scores -> Array[UScore]
    */
    var <>objects;
    
    *initClass { all = [] }

    *new{ |tracks|
        ^super.newCopyArgs(tracks).addToAll;
    }
    
    addToAll { if( all.includes( this ).not ) { all = all.add( this ) }; }
    
    makeCurrent { current = this  }
     
    at { |index| ^objects[ index ] }

    startAll {
		objects.do(_.start);
    }

    /*
    *   server: Server or Array[Server]
    */
    startChains { |server|
        var chains = objects.select({ |obj| obj.class == UChain });
        fork{
            chains.do( _.prepare(server) );
            server.sync;
            chains.do( _.start(server) );
        };
        ^chains
    }
    
     startScores { |server|
        var scores = objects.select({ |obj| obj.class == UScore });
        fork{
            scores.do( _.prepare(server) );
            server.sync;
            scores.do( _.start(server) );
        };
        ^scores
    }

}