USession : UArchivable{

    /*
    *   objects -> Array[UChain,UChainGroup,UScore or UScoreList]
    */
    var <objects, <name = "untitled";

    *new{ |...objects|
        ^super.new.init(objects);
    }

    init { |inObjects|
        objects = if(inObjects.size > 0){ inObjects }{ [] };
    }

    *current { ^USessionGUI.current !! _.session }

    at { |index| ^objects[ index ] }

    add { |item|
        objects = objects.add(item);
        this.changed(\objectsChanged)
    }

    remove { |item|
        objects.remove(item);
        this.changed(\objectsChanged)
    }

    startAll { |targets|
		objects.do(_.prepareAndStart);
    }

    startChains { |targets|
        var chains = objects.select(_.isUChainLike);
        chains.do(_.prepareAndStart(targets))
        ^chains
    }
    
    startScores { |targets|
        var scores = objects.select(_.isUScoreLike);
        scores.do(_.prepareAndStart(targets))
        ^scores
    }

    stopAll {
        objects.do(_.release)
    }

    gui { ^USessionGUI(this) }

    getInitArgs {
		^objects;
	}

	storeArgs { ^this.getInitArgs }

	onSaveAction { this.name = filePath.basename.removeExtension }

	name_ { |x| name = x; this.changed(\name) }

}