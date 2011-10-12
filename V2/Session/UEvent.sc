UEvent {

    var <startTime=0;
    var <>track=0;  //track number (horizontal segment) on the score editor
    var <duration = inf;
    var <>muted = false;
    var <releaseSelf = true;

    /*
    If 'releaseSelf' is set to false, then uchains will not free themselves automatically when the events stop playing.
    If 'releaseSelf' is set to true, then uchains will free themselves even if the score is paused;
	*/
    
    // event duration of non-inf will cause the event to release its chain
    // instead of the chain releasing itself

	waitTime { this.subclassResponsibility(thisMethod) }
	prepareTime { ^startTime - this.waitTime } // time to start preparing
	
	<= { |that| ^this.prepareTime <= that.prepareTime } // sort support

    duration_{ this.subclassResponsibility(thisMethod) }
    isPausable_{ this.subclassResponsibility(thisMethod) }
    
    startTime_ { |newTime|
	   startTime = newTime; 
	   this.changed( \startTime )
    }

    endTime { ^startTime + this.duration; } // may be inf
    eventEndTime { ^startTime + this.eventSustain }

	mute { this.muted_(true) }
	unMute { this.muted_(false) }
	toggleMute { this.muted_(muted.not) }

    /*
    *   server: Server or Array[Server]
    */
    play { |server| // plays a single event plus waittime
        ("preparing "++this).postln;
        this.prepare(server);
        fork{
            this.waitTime.wait;
            ("playing "++this).postln;
            this.start(server);
            if( duration != inf ) {
	           this.eventSustain.wait;
	           this.release;
            };
        }
    }
    
    archiveAsCompileString { ^true }
    
    getInitArgs { this.subclassResponsibility(thisMethod) }
    
    *readTextArchive { |pathname|
	    var res, sub;
	    res = pathname.load;
	    sub = this.subclasses;
	    sub = sub ? [];
	    if( res.class == this or: { this.subclasses.includes( res.class ) } ) {
		   ^res;
	    } {
		    "%:readTextArchive - wrong type (%)\n".postf( this, res );
		    ^nil;
	    }
    }
    
    readTextArchive { |pathname|
	    var res;
	    res = this.class.readTextArchive( pathname );
	    if( res.notNil ) {
		    this.init( res.getInitArgs );
	    };
    }
    
    write { |path, overwrite=false, ask=true|
	    var writeFunc;
	    writeFunc = { |overwrite, ask|
		    var text;
		    text = this.asTextArchive;
		    File.checkDo( path, { |f|
				f.write( text );
			}, overwrite, ask);
	    };

	    if( path.isNil ) {
		    Dialog.savePanel( { |pth|
			    path = pth;
			    writeFunc.value(true,false);
		    } );
	    } {
		    writeFunc.value(overwrite,ask);
	    };
    }
    
    read { |path, action|
         var score;

        if( path.isNil ) {
		    Dialog.getPaths( { |paths|
	             this.readTextArchive( paths[0] );
	             action.value(score);
	        });
	    } {
	            path = path.standardizePath;
	            this.readTextArchive( path );
	            action.value(score);
	    };
    }
    
    *read { |path, action|
        var score;

        if( path.isNil ) {
		    Dialog.getPaths( { |paths|
	             score = this.readTextArchive( paths[0] );
	             action.value(score);
	             score
	        });
	    } {
	            path = path.standardizePath;
	            score = this.readTextArchive( path );
	            action.value(score);
	            ^score
	    };
    }

}