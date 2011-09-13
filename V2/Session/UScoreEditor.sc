UScoreEditor {

	classvar <>current, <all;

	var <score;
	var <undoStates, <redoStates, maxUndoStates = 40;
	var <dirty = false;

	*new { |score|
		^super.newCopyArgs( score)
			.init
			.addToAll
	}

	init {
		undoStates = List.new;
		redoStates = List.new;
	}

	addToAll {
		all = all.asCollection.add( this );
	}

	removeFromAll {
	    if( all.notNil ) {
	        all.remove( this )
	    };
	}

	events { ^score.events }

	//--UNDO/REDO--
	storeUndoState {

		dirty = true;
		redoStates = List.new;
		undoStates.add( score.duplicate );
		if(undoStates.size > maxUndoStates) {
			undoStates.removeAt(0);
		}

	}

	undo {

		if(undoStates.size > 0) {
			redoStates.add(score);
			score = undoStates.pop;
		};
		this.changed(\score);
		this.changed(\undo);

	}

	redo {

		if( redoStates.size > 0 ) {
			undoStates.add(score);
			score = redoStates.pop;
		};
		this.changed(\score);
		this.changed(\redo);

	}

	save{
		if(score.filePath.notNil){
			score.writeUFile( score.filePath ,true, false);
			dirty = false;
		}{
			this.saveAs
		}
	}

	saveAs{ |path|
		score.writeUFile( path );
		dirty = false;
		score.filePath = path;
	}

    //--SCORE IMPORTING--

	importScorePlaceAtPos { |score, pos = 0|
		score <| score.events.collect({ |event|
	        event.startTime = pos + event.startTime;
		});
		score.cleanOverlaps;
		this.changed(\score);
	}

	importScoreAsFolder { |score|
	    score <| Event(score, 0 );
		score.cleanOverlaps;
		this.changed(\score);
	}

    //--EVENT MANAGEMENT--

    <| { |events|
        this.storeUndoState;
        score <| events;
        this.changed(\score);
    }

	duplicateEvents{ |events|
		this.storeUndoState;
	    events.do({ |ev| score.addEventToCompletelyEmptyTrack( ev.duplicate ) } );
		this.changed(\score);
	}

	deleteEvents { |events|
		this.storeUndoState;
		events.do( score.events.remove(_) );
		this.changed(\score);
	}

	muteEvents { |events|
		this.storeUndoState;
		events.do( _.mute );
		this.changed(\score);
	}

	unmuteEvents { |events|
		this.storeUndoState;
		events.do( _.unMute );
		this.changed(\score);
	}

	toggleMuteEvents { |events|
	    this.storeUndoState;
	    events.do( _.toggleMute );
	    this.changed(\score)
	}

	unmuteAll {
		this.unmuteEvents(score.events);
		this.changed(\score);
	}

	soloEvents { |events|
		this.storeUndoState;
		if( events.size > 0 ) {
			score.events.do({ |event|
				if( events.includes( event ) ) {
					event.unMute
				} {
					event.mute
				};
			});
		};
		this.changed(\score);
	}

	folderFromEvents { |events|
		var folderEvents, folderStartTime;
		this.storeUndoState;
		if( events.size > 0 ) {
			events.do({ |item|
				score.events.remove( item );
			});
			folderStartTime = events.sort[0].startTime;
			score.events = score.events.add(
                UEvent( folderStartTime,
                    UScore(
                        *events.collect({ |event|
                            event.startTime_( event.startTime - folderStartTime )
                        }) ),
                    events[0].track
                )
			);
            this.changed(\score);
		};
	}

	unpackSelectedFolders{ |events|
		var folderEvents,newEvents;

		newEvents = [];

		if( events.size > 0 and: { folderEvents = events.select( _.isFolder );
				folderEvents.size > 0
				}
		) {
			this.storeUndoState;
			folderEvents.do({ |folderEvent|
				newEvents = newEvents
					++ folderEvent.object.events.collect({ |item|
						item.startTime_( item.startTime + folderEvent.startTime )
					});
				score.events.remove( folderEvent );
			});

			newEvents.do({ |evt|
					score.addEventToCompletelyEmptyTrack( evt );
			});

			this.changed(\score);
		}
	}

	cutEventsStart { |events,pos,isFolder=false,removeFadeIn = true|
        this.storeUndoState;
		events.do{ |ev|
		    ev.cutStart(pos, false, removeFadeIn)
		};
		this.changed(\score);
	}

	cutEventsEnd { |events,pos, isFolder = false, removeFadeOut = false|
        this.storeUndoState;
		events.do{ |ev|
		    ev.cutEnd(pos, removeFadeOut)
		};
		this.changed(\score);
	}

	splitEvents { |events, pos|
		var frontEvents, backEvents;

		this.storeUndoState;
		frontEvents = events.select({ |event|
			var dur = event.dur;
			var start = event.startTime;
 			(start < pos) && ((start + dur) > pos)
		});
		backEvents = frontEvents.collect(_.duplicate);
		score.events = score.events ++ backEvents;
		this.cutEventsStart(backEvents, pos, removeFadeIn:true);
		this.cutEventsEnd(frontEvents, pos, removeFadeOut:true);
		this.changed(\score);
	}

    //perform editing operations at the current transport position
	trimEventsStartAtPos{ |events|
		this.cutEventsStart(events, score.pos);
	}

	trimEventsEndAtPos{ |events|
		this.cutEventsEnd(events, score.pos);
	}

	splitEventsAtPos { |events|
	    this.splitEvents(events, score.pos)
	}

}