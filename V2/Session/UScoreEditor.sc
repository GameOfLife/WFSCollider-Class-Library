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

	changeScore{ |action|
	    this.changed(\preparingToChangeScore);
	    this.storeUndoState;
	    action.value;
		this.changed(\score);
	}

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
	    this.changeScore({
		score <| score.events.collect({ |event|
	        event.startTime = pos + event.startTime;
		});
		    score.cleanOverlaps;
		})
	}

	importScoreAsFolder { |score|
	    this.changeScore({
	        score <| Event(score, 0 );
		    score.cleanOverlaps;
		})
	}

    //--EVENT MANAGEMENT--

    <| { |events|
        this.changeScore({
            score <| events;
        })
    }

	duplicateEvents{ |events|
		this.changeScore({
	        events.do({ |ev| score.addEventToCompletelyEmptyTrack( ev.duplicate ) } );
		})
	}

	deleteEvents { |events|
		this.changeScore({
		    events.do( score.events.remove(_) );
		})
	}

	muteEvents { |events|
		this.changeScore({
		    events.do( _.mute );
		})
	}

	unmuteEvents { |events|
		this.changeScore({
		    events.do( _.unMute );
		})
	}

	toggleMuteEvents { |events|
	    this.changeScore({
	        events.do( _.toggleMute );
	    })
	}

	unmuteAll {
	    this.changeScore({
		    this.unmuteEvents(score.events);
		})
	}

	soloEvents { |events|
		this.changeScore({
            if( events.size > 0 ) {
                score.events.do({ |event|
                    if( events.includes( event ) ) {
                        event.unMute
                    } {
                        event.mute
                    };
                });
            };
		})
	}

	folderFromEvents { |events|
		var folderEvents, folderStartTime;

		if( events.size > 0 ) {
		    this.changeScore({
                events.do({ |item|
                    score.events.remove( item );
                });
                folderStartTime = events.sort[0].startTime;
                score.events = score.events.add(
                    UScore(
                        *events.collect({ |event|
                            event.startTime_( event.startTime - folderStartTime )
                        })
                    ).startTime_(folderStartTime).track_(events[0].track)
                );
            })
		};
	}

	unpackSelectedFolders{ |events|
		var folderEvents,newEvents;

		newEvents = [];

		if( events.size > 0 and: { folderEvents = events.select( _.isFolder );
				folderEvents.size > 0
				}
		) {
			this.changeScore({
                folderEvents.do({ |folderEvent|
                    newEvents = newEvents
                        ++ folderEvent.events.collect({ |item|
                            item.startTime_( item.startTime + folderEvent.startTime )
                        });
                    score.events.remove( folderEvent );
                });

                newEvents.do({ |evt|
                        score.addEventToEmptyTrack( evt );
                });

			})
		}
	}

	cutEventsStart { |events,pos,isFolder=false,removeFadeIn = true|
        this.changeScore({
            events.select(_.isFolder.not).do{ |ev|
                ev.cutStart(pos, false, removeFadeIn)
            };
		})
	}

	cutEventsEnd { |events,pos, isFolder = false, removeFadeOut = false|
        this.changeScore({
            events.select(_.isFolder.not).do{ |ev|
                ev.cutEnd(pos, removeFadeOut)
            };
		})
	}

	splitEvents { |events, pos|
		var frontEvents, backEvents;

		this.changeScore({
		frontEvents = events.select(_.isFolder.not).select({ |event|
			var dur = event.dur;
			var start = event.startTime;
 			(start < pos) && ((start + dur) > pos)
		});
		backEvents = frontEvents.collect(_.duplicate);
		score.events = score.events ++ backEvents;
		backEvents.do{ |ev|
                ev.cutStart(pos, false, true)
            };
		frontEvents.do{ |ev|
                ev.cutEnd(pos, true)
            };
		})
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