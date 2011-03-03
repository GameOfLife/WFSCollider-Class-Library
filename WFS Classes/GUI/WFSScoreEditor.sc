/*
    GameOfLife WFSCollider - Wave Field Synthesis spatialization for SuperCollider.
    The Game Of Life Foundation. http://gameoflife.nl
    Copyright 2006-2010 Wouter Snoei, Miguel Negr‹o.

    GameOfLife WFSCollider software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GameOfLife WFSCollider is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GameOfLife WFSCollider.  If not, see <http://www.gnu.org/licenses/>.
*/

WFSScoreEditor {
	
	classvar <>current, <all;
	classvar <>askForSave = true;
	
	var <score, <isMainEditor, <parent;
	var <>window;
	var <snapActive, <>snapH, <>numTracks;
	var id;
	var <undoStates, <redoStates, maxUndoStates = 40;
	var <>wfsEventViews, <wfsMouseEventsManager;
	var views;
	var <dirty = false;

	*initClass { UI.registerForShutdown({ WFSScoreEditor.askForSave = false }); }
	
	*new { |wfsScore, isMainEditor = true, parent |
		^super.newCopyArgs( wfsScore, isMainEditor, parent)
			.init
			.addToAll
			.newWindow
			.makeCurrent;
	}
	
	*open{
		Dialog.getPaths( { |paths|	
			var score = WFSScore.readWFSFile( paths[0] ).filePath_(paths[0]);
			WFSScoreEditor( score, true, nil);		}) 
	}
	
	init { 
		undoStates = List.new;
		redoStates = List.new;
		snapActive = true;
		snapH = 0.25;
		numTracks = 16;
		this.createID;
		this.createWFSEventViews;
	}
	
	createWFSEventViews {
		wfsEventViews = score.events.collect{ |event,i|
			WFSEventView(event,i)
		};
		wfsMouseEventsManager = WFSMouseEventsManager(wfsEventViews,this);
	}
	
	//undo
	storeUndoState {
		
		dirty = true;
		if(undoStates.size == 0) {
			views[\undo].enabled_(true)
		};
		redoStates = List.new;
		views[\redo].enabled_(false);
		undoStates.add(score.duplicate);		
		if(undoStates.size > maxUndoStates) {
			undoStates.removeAt(0);
		}

	}

	undo { 
		
		if( redoStates.size == 0 ) {
			views[\redo].enabled_(true)
		};
		if(undoStates.size > 0) {
			redoStates.add(score);
			score = undoStates.pop;			
			this.createWFSEventViews;
			this.update;
		};
		if( undoStates.size == 0 ) {
			views[\undo].enabled_(false)
		};

					
	}
	
	redo {
		
		if( undoStates.size == 0 ) {
			views[\undo].enabled_(true)
		};
		if( redoStates.size > 0 ) {
			undoStates.add(score);
			score = redoStates.pop;			
			this.createWFSEventViews;
			this.update;
		};
		if( redoStates.size == 0 ) {
			views[\redo].enabled_(false)
		}
	}	
			
	update {
		if( window.window.notNil && { window.window.dataptr.notNil } ) { 
			window.refresh; };
			if( parent.notNil ) { parent.update };
			 }
			 
	toFront { if( window.window.notNil && { window.window.dataptr.notNil } ) 
		{ window.window.front; };
		 }
			
	makeCurrent { if( isMainEditor ) 
		{ current = this; WFSTransport.refreshScoreMenu; }
		{ if( parent.notNil ) { current = parent; WFSTransport.refreshScoreMenu;  } }
		}
	
	createID { 
		if( isMainEditor )
			{ id = ((all.collect({ |item| item.id ? -1 }) ? []) ++ [-1]).maxItem + 1; }
		 }
	
	id { if( isMainEditor ) { ^id } { ^nil }  } // change later?
	
	addToAll { 
		all = all.asCollection.add( this ); 
		WFSTransport.refreshScoreMenu;
	}
	
	removeFromAll { if( all.notNil ) { all.remove( this ); WFSTransport.refreshScoreMenu; }; }

	selectedEvents { ^this.selectedEventViews.collect{ |eventView| eventView.event}  }
	
	selectedEventViews { 
		^wfsMouseEventsManager.selectedEvents
	}
	
	selectedEventsOrAll {
		var events = this.selectedEvents;
		if(events.size == 0)Ê{
			^score.events
		} {
			^events
		}	
	}
	
	createWindowTitle{
		^"WFSScoreEditor ( "++
		if( isMainEditor ) {
			WFSScoreEditor.all.indexOf(this).asString++" - "++
			if( score.filePath.isNil ) {
				"Untitled )"
			} {
				PathName(score.filePath).fileName.removeExtension++" )"
			}
		} {
			if( parent.id.notNil ) {
				("folder of " ++ ( parent !? { parent.id } ))++": " ++ score.name ++ " )"
			} {
				"folder: " ++ score.name ++ " )"
			}
		};
	}
	
	setWindowTitle{
		window.window.name = this.createWindowTitle;
	}
	
	save{
		if(score.filePath.notNil){	
			score.writeWFSFile( score.filePath ,true, false);
			dirty = false;
		}{
			this.saveAs
		}
	}
	
	saveAs{
		Dialog.savePanel( { |path| 
			score.writeWFSFile( path );
			dirty = false;
			score.filePath = path;
			this.setWindowTitle;
		});
	} 
	
	combineAppend{
		Dialog.getPaths( { |paths|
			var nextFunc;
			nextFunc = { |index = 0|
				var path, newScore;
				path = paths[index];
				if( path.notNil )
					{ newScore = WFSScore.readWFSFile( path );
					  if( newScore.notNil )
					  	{ if( score.events.size == 0 )
					  		{ score = WFSScore.readWFSFile( paths[0] ) ? score;
						  		this.createWFSEventViews;
								this.update;  nextFunc.value(index+1); }
							{ SCAlert( "where do you want to place '%'?"
								.format( path.basename ),
							[ "cancel", "skip",
								"as folder", 
								"at start", "current pos", "at end"],
							[ { }, { nextFunc.value(index+1) },
								{ score.events = score.events.add(
									   WFSEvent( 0, newScore ) );
								score.cleanOverlaps;
								this.createWFSEventViews;
							   	this.update; 
							   	nextFunc.value(index+1);  },
							   { score.events = 
							   		score.events ++ newScore.events;
							   	score.cleanOverlaps;
							   	this.update; 
							   	nextFunc.value(index+1); },
							   { newScore.events.do({ |event|
									event.startTime = 
									   WFSTransport.pos + event.startTime;
									});
								score.events = score.events ++ newScore.events;
								score.cleanOverlaps;
								this.createWFSEventViews;
								this.update;
								nextFunc.value(index+1);
								},
								{ newScore.events.do({ |event|
									event.startTime = 
									   event.startTime + score.duration;
									});
								score.events = score.events ++ newScore.events;
								score.cleanOverlaps;
								this.createWFSEventViews;
								this.update;
								nextFunc.value(index+1);
								} ]	);
							};
						};
					};
				};
			nextFunc.value; });	
		
	}
	
	addAudioFiles{ 
		var copiedEvents, newTrack, newEventsSize;
		Dialog.getPaths( { |paths|
				var newEvents;
				this.storeUndoState;
				newEvents = paths.collect({ |path, i|
					var newEvent;
					newEvent = WFSEvent( 
						WFSTransport.pos, 
						WFSSynth( 
							'static_buf', 
							WFSPoint(0,0,0),
							Server.default,
							path ), i );
					newEvent.wfsSynth.useSoundFileDur;
					newEvent; });

				newEvents.do({ |evt|
					score.addEventToCompletelyEmptyTrack( evt );
				});

				this.createWFSEventViews;
				wfsEventViews.keep(newEvents.size.neg).do{ |event|
					event.selected_(true)
				};
				this.update;
				} ); 
	}
	
	addTestEvent{
		var copiedEvents, newTrack;
		this.storeUndoState; 

		score.addEventToCompletelyEmptyTrack( WFSEvent( WFSTransport.pos ) );

		this.createWFSEventViews;
		this.update;
	}
	
	duplicateSelected{
		var copiedEvents, newTrack;
		var selectedEvents = this.selectedEvents;
		this.storeUndoState;

		if( selectedEvents.size > 0 ) { 
			copiedEvents = selectedEvents.collect({ |event| 
				newTrack = event.track + 1;
				if( ( newTrack + 1 ) > numTracks ) {
					newTrack =  event.track - 1 
				};
				event.duplicate.track_( newTrack );
			});

			copiedEvents.do({ |evt|
				score.addEventToCompletelyEmptyTrack( evt );
			});

			this.createWFSEventViews;
			wfsEventViews.keep(copiedEvents.size.neg).do{ |event|
					event.selected_(true)
				};

			this.update;
			};
	}
	
	editSelected { |front = false|
		
		var selectedEvent;
		selectedEvent = this.selectedEvents[0];
		selectedEvent !?	{	
			selectedEvent.edit( window.window.bounds.rightTop + (5@0), parent: this, toFront: front );
			if( selectedEvent.isFolder )
				{ selectedEvent.wfsSynth.edit( this ); }; };
	}
	
	deleteSelected {
		this.storeUndoState;
		this.selectedEventViews.do({ |eventView|
			score.events.remove(eventView.event);
			wfsEventViews.remove(eventView);
		});		
		this.update;		
	}
	
	selectAll { 
		wfsEventViews.do({ |eventView| eventView.selected = true });
		this.update;
	}
	
	selectSimilar{
		var selectedTypes = this.selectedEvents
			.collect({ |event| 
				(event.wfsSynth.audioType.asString ++ "_" ++ event.wfsSynth.intType).asSymbol
			});
		wfsEventViews.do({ |eventView|
			if(selectedTypes.includes( 
				(eventView.event.wfsSynth.audioType.asString ++ "_" ++ eventView.event.wfsSynth.intType).asSymbol
			)) { eventView.selected = true }
		});
		this.update;		
	}
	
	muteSelected {
		this.storeUndoState;
		this.selectedEvents.do( _.mute );
		this.update;
	}
	
	unmuteSelected {
		this.storeUndoState;
		this.selectedEvents.do( _.unMute );
		this.update;	
	}
	
	unmuteAll {
		this.storeUndoState;
		score.events.do( _.unMute );
		this.update;
	}
	
	soloSelected {
		var selectedEvents = this.selectedEvents;
		this.storeUndoState;
		if( selectedEvents.size > 0 ) { 
			score.events.do({ |event|
				if( selectedEvents.includes( event ) ) {
					event.unMute
				} { 
					event.mute
				};
			});
			this.update;
		};
	}
	
	addTrack {
		numTracks = numTracks + 1; 
		this.update;
	}
		
	removeUnusedTracks {
		numTracks = ((score.events.collect( _.track )
			.maxItem ? 14) + 2).max( 16 );
		this.update;
	}	
	
	folderFromSelectedEvents {
		var folderEvents, folderStartTime;
		var selectedEvents = this.selectedEvents;
		this.storeUndoState;
		if( selectedEvents.size > 0 ) {
			selectedEvents.do({ |item|
				score.events.remove( item ); 
			});
			folderStartTime = selectedEvents.sort[0].startTime;
			score.events = score.events.add( 
					WFSEvent( folderStartTime,
						WFSScore(
							*selectedEvents.collect({ |event|
								event.startTime_( 
									event.startTime - folderStartTime )
								}) ),
						selectedEvents[0].track )
					);
			this.createWFSEventViews;
			this.update;
		
		} {
			SCAlert( "Sorry, no events selected.\nUse shift to select multiple." )
		}
	}
	
	unpackSelectedFolders{
		var folderEvents,newEvents;
		var selectedEvents = this.selectedEvents;
		
		newEvents = [];
		
		if( selectedEvents.size > 0 and: { folderEvents = selectedEvents.select( _.isFolder );
				folderEvents.size > 0  
				}
		) {
			this.storeUndoState;
			folderEvents.do({ |folderEvent|
				newEvents = newEvents
					++ folderEvent.wfsSynth.events.collect({ |item|
						item.startTime_( item.startTime + folderEvent.startTime )
					});
				score.events.remove( folderEvent );
			}); 
			
			newEvents.do({ |evt|
					score.addEventToCompletelyEmptyTrack( evt );
			});
			
			this.createWFSEventViews;
			this.update;
		} { 
			SCAlert( "Sorry, no folders selected." ) 
		}	
	}
	
	cutEventsStart { |events,pos,isFolder=false,removeFadeIn = false|
			
		events.do{ |event|
			var dur = event.dur;
			var start = event.startTime;
			if((start < pos) && ((start + dur) > pos) ) {
				if(event.wfsSynth.class == WFSSynth ) {
					
					event.trimStart(pos,true);						
					if(isFolder){
						event.startTime = 0;
					};
					
				}{	
					event.wfsSynth.events = event.wfsSynth.events.reject({ |ev|
						ev.endTime < (pos-start)
					});
					this.cutEventsStart(event.wfsSynth.events,pos-start,true,removeFadeIn);
					event.startTime = pos;
					if(isFolder){
						event.startTime = 0;
					};											}
			}
		}
	}
		
	trimEventsStartAtPos{ |onlySelected = true|
		var events = if(onlySelected) {
			this.selectedEventsOrAll
		} {
			score.events
		};
		this.storeUndoState;			
		this.cutEventsStart(events,WFSTransport.pos);
		this.update;
	}
		
	cutEventsEnd { |events,pos,isFolder = false, removeFadeOut = false|
	
		events.do{ |event|
			var dur = event.dur;
			var start = event.startTime;

			if((event.startTime < pos) && (( event.startTime + event.dur ) > pos) ) {
				
				if(event.isFolder) {
					
					event.wfsSynth.events = event.wfsSynth.events.reject({ |ev|
						ev.startTime > ( pos - start )
					});
									
					this.cutEventsEnd(event.wfsSynth.events,pos - event.startTime,true,removeFadeOut);
				}{					
					event.trimEnd(pos,true);				
				}			
			} 
		}
	}	
	
	trimEventsEndAtPos{ |onlySelected = true|
		var events = if(onlySelected) {
			this.selectedEventsOrAll
		} {
			score.events
		};
		this.storeUndoState;	
		this.cutEventsEnd(events,WFSTransport.pos);
		this.update;
	}
	
	splitEventsAtPos{
		var frontEvents, backEvents;
		this.storeUndoState;
		frontEvents = this.selectedEventsOrAll.select({ |event|
			var dur = event.dur;
			var start = event.startTime;
			var pos = WFSTransport.pos;
 			(start < pos) && ((start + dur) > pos)
		});
		backEvents = frontEvents.collect(_.duplicate);
		score.events = score.events ++ backEvents;
		this.cutEventsStart(backEvents,WFSTransport.pos,removeFadeIn:true);
		this.cutEventsEnd(frontEvents,WFSTransport.pos,removeFadeOut:true);
		this.createWFSEventViews; 
		this.update;		

	}

	checkSoundFiles {
		var errorString = "";
		score.checkSoundFile( 
			 { |synth, sf| errorString = 
			 	errorString ++ "soundfile '" ++ 
			 		sf.path.deStandardizePath ++ "' has > 1 channels\n" },
			 { |synth, sf| errorString = 
			 	errorString ++ "soundfile '" ++
			 		sf.path.deStandardizePath ++ "' sampleRate != 44100\n" },
			 { |synth, sf| errorString = 
			 	errorString ++ "soundfile '" ++ 
			 		sf.path.deStandardizePath ++ "' could not be opened\n" }
		);
		if( errorString.size > 0 ) {
			Document( "WFSScoreEditor:check all soundfiles - Report", errorString ); 
		} {
			"\nWFSScoreEditor:check all soundfiles - no problems found".postln
		}
	}
	
	copySoundFiles {
		var copyToFolderFunc = {
			Dialog.savePanel({ |path|
				//score.copySoundFileTo( path.dirname, 
				// doneAction: { this.update } )
				var filePaths, duplicates, dupString;
				filePaths = score.collectFilePaths;
				duplicates = score.detectDupFileNames( filePaths );
				SCAlert( "There are % soundfiles used in this score".format( filePaths.size ) ++
					"\nof which % have one or more duplicate\nnames in different folders.".format( duplicates.keys.size ) ++
					"\n\nDo you really want to copy to\n'%/'?".format( path ),
					[ "cancel", "inspect", "change folder", "ok" ],
					[ { },
				 	{ dupString = "WFSScore duplicate file report:\n";
				  	duplicates.sortedKeysValuesDo({ |key, value|
				  		dupString = dupString ++ key ++ ":\n\t";
				  		dupString = dupString ++ 
				  			filePaths.detect({ |item| 
				  				item.asString.basename.asSymbol == key }) ++ "\n";
				  		value.do({ |item| dupString = 
				  			dupString ++ "\t" ++ item ++ "\n" });
				  		});
				   	Document.new.string_( dupString );
				  	},
				  	copyToFolderFunc,
				  	{ score.copyFilesToFolder( path, doneAction: { this.update } ); } ] 
				);
			});
		};
		copyToFolderFunc.value;
	}
	
	plotAtTimeline { |all = false|
		var events, paths, pos;
		
		pos = WFSTransport.pos;
		
		events = if( all ) {
			score.allEvents.select{ |event|
				event.wfsSynth.wfsPath.notNil
			}
		}{
			score.events.select{ |event|
				event.isFolder.not and: {event.wfsSynth.wfsPath.notNil }
			}
		};
		
		events = events.select{ |event|
			var dur = event.dur;
			var start = event.startTime;
			(start < pos) && ((start + dur) > pos)
		};	
		
		paths = events.collect{ |event|
			event.wfsSynth.wfsPath
				.currentTime_( event.startTime.neg + WFSTransport.pos )
		};
				
		WFSMixedArray.with(*paths).plotSmooth(events:events); 
	}		

	plot{ |all = false|
		var events, paths;
		
		events = this.selectedEvents;
		
		if(events.size == 0){
			events = score.events;
		};
		
		events = if( all ) {
			WFSScore.allEvents(events).select{ |event|
				event.wfsSynth.wfsPath.notNil
			}
		}{
			events.select{ |event|
				event.isFolder.not and: {event.wfsSynth.wfsPath.notNil }
			}
		};
		paths = events.collect{ |event|
			event.wfsSynth.wfsPath
				.currentTime_( event.startTime.neg + WFSTransport.pos )
		};
				
		WFSMixedArray.with(*paths).plotSmooth(events:events); 
	}
					
	newWindow {		
		
		var font = Font( Font.defaultSansFace, 11 ), header, windowTitle;	
		views = ();
		
		numTracks = ((score.events.collect( _.track ).maxItem ? 14) + 2).max(16);
			
		window = ScaledUserView.window(this.createWindowTitle, 
			Rect(230 + 20.rand2, 230 + 20.rand2, 680, 300),
			fromBounds: Rect( 0, 0, score.duration.ceil.max(1), numTracks ),
			viewOffset: [4, 27] );
			
		window.userView.background = Color.gray(0.8);
			 
		if( isMainEditor.not ) { window.window.view.background_( Color.gray(0.33).alpha_(0.5 ) ) };
			
		window.drawHook = { this.makeCurrent };
		
		window.onClose = { 
			
			this.removeFromAll;
			
			{ 0.1.wait;  
			if( askForSave && isMainEditor ) {
				if( dirty ) {
					if( score.events.size != 0 ) {
						SCAlert( "Do you want to save your score? (" ++ id ++ ")" , 
							[ [ "Don't save" ], [ "Cancel" ], [ "Save" ] ], 
							[ 	nil, 
								{ WFSScoreEditor( score ); },  
								{ Dialog.savePanel( 
									{ |path| score.writeWFSFile( path ); },
									{ WFSScoreEditor( score ); }); } 
							] ); 
					};
				}
			}
			}.fork( AppClock ); // prevent crash at shutdown
			
		};
		
		window.userView.gridLines = [score.duration.ceil,max(1), numTracks];
		window.userView.gridMode = ['blocks','lines'];
		window.maxZoom = [16,5];
		
		//window.window.acceptsMouseOver_( true );
		
		header = CompositeView( window.window, Rect(0,0, window.window.view.bounds.width, 25 ) );
		header.addFlowLayout;
		//header.background_( Color.gray(0.95) );
		//header.resize_(2);
        
		SmoothButton( header, 18@18 )
			.states_( [[ \i, Color.black, Color.blue.alpha_(0.125) ]] )
			.canFocus_(false)
			.border_(1)
			.border_(1)
			.action_({ |b|
				this.editSelected(true)
			});				
			
		header.decorator.shift(10);    
            
		SmoothButton( header, 18@18 )
			.states_( [[ '-' ]] )
			.canFocus_(false)
			.border_(1)
			.action_({ 
				this.deleteSelected				
			});
			
		SmoothButton( header, 18@18 )
			.states_( [[ '+' ]] )
			.canFocus_(false)
			.border_(1)			
			.action_({ 
				if( this.selectedEvents.size > 0 )
					{ this.duplicateSelected } 
					{ this.addAudioFiles }		
			});
			
		header.decorator.shift(10);	
		
		SmoothButton( header, 18@18  )
 			.states_( [[ "[", Color.black, Color.clear ]] )
 			.canFocus_(false)
			.radius_( 0 )
			.border_(1)
			.font_( Font( font.name, 10 ).boldVariant )
			.radius_([8,0,0,8])
			.action_({ 
				this.trimEventsStartAtPos			
			});
			
		SmoothButton( header, 18@18  )
			.states_( [[ "|", Color.black, Color.clear ]] )
			.canFocus_(false)
			.radius_(0)
			.border_(1)
			.action_({ 
				this.splitEventsAtPos				
			});	
			
		SmoothButton( header, 18@18  )
			.states_( [[ "]", Color.black, Color.clear ]] )
			.canFocus_(false)
			.radius_([0,8,8,0])
			.border_(1)
			.action_({ 
				this.trimEventsEndAtPos				
			});	
			
		header.decorator.shift(10);
		
		views[\undo] = SmoothButton( header, 18@18 )
			.states_( [[ 'arrow_pi' ]] )
			.canFocus_(false)
			.border_(1)
			.enabled_(false)
			.action_({ 
				this.undo		
			});
			
		views[\redo] = SmoothButton( header, 18@18 )
			.states_( [[ 'arrow' ]] )
			.canFocus_(false)
			.border_(1)
			.enabled_(false)
			.action_({ 
				this.redo		
			});
		
		header.decorator.shift(10);		
		
		SmoothButton( header, 18@18  )
			.states_( [[ \speaker, Color.black, Color.clear ]] )
			.canFocus_(false)
			.border_(1)
			.action_({ |b|
				this.storeUndoState;
				this.selectedEvents.do( _.toggleMute );
				this.update; 
			});
				
		SmoothButton( header, 18@18  )
			.states_( [[ \folder, Color.black, Color.clear ]] )
			.canFocus_(false)
			.border_(1)
			.action_({ 
				if( this.selectedEvents.every(_.isFolder) ) {
					this.unpackSelectedFolders
				}{
					this.folderFromSelectedEvents; 
				};
			});
				
		header.decorator.shift(10);	
				
		SmoothButton( header, 40@18  )
			.states_( [[ "mixer", Color.black, Color.clear ]] )
			.canFocus_(false)
			.font_( font )
			.border_(1)
			.action_({ |b|
				WFSMixer(this.selectedEventsOrAll,List.new);
			});
			
		SmoothButton( header, 40@18  )
			.states_( [[ "batch", Color.black, Color.clear ]] )
			.canFocus_(false)
			.font_( font )
			.border_(1)
			.action_({ 
				WFSBatch.new 
			});
			
		header.decorator.shift(10);		

		SmoothButton( header, 40@18  )
			.states_( [[ "plot", Color.black, Color.clear ]] )
			.canFocus_(false)
			.font_( font )
			.border_(1)
			.action_({ 
				this.plot;
			});		
				
		SmoothButton( header, 50@18  )
			.states_( [[ "plot all", Color.black, Color.clear ]] )
			.canFocus_(false)
			.font_( font )
			.border_(1)
			.action_({ 
				this.plot(true);
			});
		
		StaticText( header, 30@18 ).string_( "snap" ).font_( font ).align_( \right );
				
		PopUpMenu( header, 50@18 )
			.items_( [ "off", "0.001", "0.01", "0.1", "0.25", "0.333", "1" ] )
			.canFocus_(false)
			.font_( font )
			.value_(4)
			.action_({ |v|
				if (v.value == 0)
					{ snapActive = false; }
					{ snapActive = true; };
					
				snapH = [0, 0.001, 0.01, 0.1, 0.25, 1/3, 1][ v.value ];
				});
				
		StaticText( header, 10@18 ).string_( "s" ).font_( font );
		
		header.decorator.shift(4);
		
		StaticText( header, 30@18 ).string_( "Mode:" ).font_( font );
				
		PopUpMenu( header, 50@18 )
			.items_( [ "all","move","resize","fades"] )
			.canFocus_(false)
			.font_( font )
			.value_(0)
			.action_({ |v|
				wfsMouseEventsManager.mode = v.items[v.value].asSymbol;
			});

		
		window.userView
			.mouseDownAction_( { |v, x, y,mod,x2,y2| 	 // only drag when one event is selected for now
				var scaledPoint, shiftDown,altDown;
				
				scaledPoint = [ x,y ].asPoint;
				shiftDown = ModKey( mod ).shift( \only );
				altDown = ModKey( mod ).alt( \only );
				
				wfsMouseEventsManager.mouseDownEvent(scaledPoint,Point(x2,y2),shiftDown,altDown,v);

			} )				
			.mouseMoveAction_( { |v, x, y, mod, x2, y2, isInside| 
				var snap = if(snapActive){snapH * v.gridSpacingH}{0};
				var shiftDown = ModKey( mod ).shift( \only );
				wfsMouseEventsManager.mouseMoveEvent(Point(x,y),Point(x2,y2),v,snap, shiftDown);

			} )				
			.mouseUpAction_( { |v, x, y, mod, x2, y2, isInside|

				var shiftDown = ModKey( mod ).shift( \only );
				
				wfsMouseEventsManager.mouseUpEvent(Point(x,y),Point(x2,y2),shiftDown,v,isInside);
	
			} )			
			.keyDownAction_( { |v, a,b,c|
				if( c == 127 ) {
					this.deleteSelected
				}
			})				
			.beforeDrawFunc_( {
				numTracks = ((score.events.collect( _.track ).maxItem ? ( numTracks - 2)) + 2)
					.max( numTracks );
				window.userView.fromBounds = Rect( 0, 0, score.duration.ceil.max(1), numTracks );
				window.userView.gridLines = [score.duration.ceil.max(1), numTracks];
				} )

			.unscaledDrawFunc_( { |v|
				var scPos, rect;
				rect = v.view.drawBounds.moveTo(0,0);
				//draw border
				GUI.pen.use({	 
					GUI.pen.addRect( rect.insetBy(0.5,0.5) );
					GUI.pen.fillColor = Color.gray(0.7).alpha_(0.5);
					GUI.pen.strokeColor = Color.gray(0.1).alpha_(0.5);
					GUI.pen.fill;
				});
				
				Pen.font = Font( Font.defaultSansFace, 10 );												
				//draw events
				wfsEventViews.do({ |wfsEventView|
					wfsEventView.draw(v);
				});	
				
				//draw selection rectangle
				if(wfsMouseEventsManager.selectionRect.notNil) {
					Pen.color = Color.white;
					Pen.addRect(v.translateScale(wfsMouseEventsManager.selectionRect));
					Pen.stroke;					
					Pen.color = Color.grey(0.3).alpha_(0.4);
					Pen.addRect(v.translateScale(wfsMouseEventsManager.selectionRect));
					Pen.fill;
				};
				
				//draw Transport line
				Pen.width = 2;			
				Pen.color = Color.black.alpha_(0.5);
				scPos = v.translateScale( WFSTransport.pos@0 );
				Pen.line( (scPos.x)@0, (scPos.x)@v.bounds.height);
				Pen.stroke;	
				
				Pen.width = 1;
				Color.grey(0.5,1).set;
				Pen.strokeRect( rect.insetBy(0.5,0.5) );
							
						
		})
							
	}
}		