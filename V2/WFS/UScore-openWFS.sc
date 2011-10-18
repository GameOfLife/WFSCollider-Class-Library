+ UScore{

	*openWFS{ |path, action|
	    var score;
        var f = { |path,action|

		         if( File(path,"r").readAllString[..8] == "<xml:wfs>") {
                    score = WFSScore.readWFSFile(path).asUEvent;
                    action.value(score);
                    score
	             } {
	                score = this.readTextArchive( path );
	                action.value(score);
	                score
	             }

        };
        if( path.isNil ) {
		    Dialog.getPaths( { |paths|
		        f.(paths[0],action);
		    });
	    } {
            path = path.standardizePath;
            f.(path,action)
	    };
	}
}