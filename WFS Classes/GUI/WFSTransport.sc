/*
    GameOfLife WFSCollider - Wave Field Synthesis spatialization for SuperCollider.
    The Game Of Life Foundation. http://gameoflife.nl
    Copyright 2006-2010 Wouter Snoei.

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

WFSTransport {
	classvar <window, <scoreMenu;
	classvar <>additionalResponders;
	classvar <>nowPlaying;
	classvar <>autoReturnToZero = false;
	classvar <>autoSwitchToNext = false;
	classvar <>advancePosRoutine;
	
	classvar <>endAction; // NEW ADDITION
	
	classvar <>videoAddr;
	classvar <>videoUpdateRate = 30;
	
	*new {  ^super.new.init; }
	
	*initClass { videoAddr = NetAddr( "192.168.2.13", 7004 ); }
	
	*refreshScoreMenu {
		if( window.notNil && { window.isClosed.not } ) 
			{ scoreMenu.items_( [ "current: " ++ 
					(WFSScoreEditor.current ? ( id: "none" )).id.asString] 
				++ WFSScoreEditor.all.select({ |item| item.id.notNil })
					.collect({ |item| item.id.asString }); )
			};

		}
	
	*pos { if( window.notNil && { window.isClosed.not } )
				{ ^window.pos } { ^0 }
			}
			
	*pos_ { |newPos| if( window.notNil && { window.isClosed.not } )
				{ window.pos = newPos } { this.new; this.pos = newPos };
			if( (  WFS.graphicsMode === \fast ) && { WFSScoreEditor.current.notNil } ) 
					{ WFSScoreEditor.current.update; }; 
			}
			
	*play {  if( window.notNil && { window.isClosed.not } )
				{ window.play };
		}
		
	*stop {  if( window.notNil && { window.isClosed.not } )
				{ window.stop };
		}
	
	*return {  if( window.notNil && { window.isClosed.not } )
				{ window.return };
		}
	
	*toFront {   if( window.notNil && { window.isClosed.not } )
		{ window.window.front };
		}
	
	init { 
		var loading, resp;
		var loadViews;
		if( window.notNil && { window.isClosed.not } ) { window.close };
		window = TransportWindow( "WFSTransport" );
		
		loadViews = ( 
			wait: WaitView( window.window, Rect(
				window.window.bounds.width - 35, 0, 30, 30 ) )
					//.speed_( 1.025 / WFSEvent.wait )
					.alphaWhenStopped_( 0 ),
			text: SCStaticText( window.window,  Rect(
				window.window.bounds.width - 130, 0, 120, 40 ) )
					.string_( "" ) 
					.font_( Font( "Monaco", 9 ) )
					.stringColor_( Color.red )
				);

		
		window.counterAction_( {  if( WFSScoreEditor.current.notNil ) 
									{ WFSScoreEditor.current.update; }; } );
		
		window.returnAction_( {  
				this.class.pos = 0;
				/* if( WFSScoreEditor.current.notNil ) 
						{ WFSScoreEditor.current.update; }; */
				 } );

		window.playAction = { |tw|
			WFSTransport.nowPlaying = WFSScoreEditor.current;
			
			WFSScoreEditor.current.score.play2(tw.pos);
			advancePosRoutine = Routine({
				var delta = 0.2;
				loadViews[ \text ].string_( "loading....." )
                    .stringColor_( Color.red(0.75) );
                loadViews[ \wait ].start;
				WFSEvent.wait.wait;
				loadViews[ \text ].string_( "playing" )
                    .stringColor_( Color.green(0.5) );
                loadViews[ \wait ].stop; 
                inf.do{
					delta.wait;
					tw.pos = tw.pos + delta;
					tw.update;
					if( WFSTransport.nowPlaying.notNil ){
						if( window.pos > WFSTransport.nowPlaying.score.duration.ceil ) {
							window.stop;
                            if( autoSwitchToNext ) {
                               WFSScoreEditor.makeNextCurrent;
                             };
						} { 
                            if( WFS.graphicsMode === \fast ) {
                            	WFSTransport.nowPlaying.update;
                            };
                     	};
					}
                         
				}
			}).play(AppClock);
					 
		};
		
		RoundButton( window.window, Rect( window.window.bounds.width - 100, 62, 95, 16 ) )
			.states_( [ 	["show position", Color.black, Color.gray(0.7)], 
						["show position", Color.gray(0.7), Color.black] ] )
			.value_( if( WFS.graphicsMode == \fast ) { 1 } { 0 } )
			.action_( { |bt| 
				if( bt.value == 1 )
					{ WFS.graphicsMode = \fast;
						//this.class.pos_( this.class.pos ); 
						}
					{ WFS.graphicsMode = \slow };
				} )
			.canFocus_( false );

		if(WFSServers.default.isSingle.not){
			RoundButton( window.window, Rect( window.window.bounds.width - 160, 62, 55, 16 ) )
				.states_( [ 	["prepare", Color.red(0.25), Color.red(0.75).alpha_(0.5) ] ] )
				.action_( { |bt| 
					WFSScore.current.writePathData;
					"WFSTransport:prepare
					All path data is now saved to a file and uploaded to both servers.
					do this once every time a new score is loaded or the score is changed".postln;
					} )
				.canFocus_( false );
		};
		
		scoreMenu = SCPopUpMenu( window.window, 
				Rect( window.window.bounds.width - 100, 80, 95, 16 ) )
			.canFocus_( false )
			.items_( [ "current: " ++ 
					(WFSScoreEditor.current ? ( id: "none" )).id.asString] 
				++ WFSScoreEditor.all.select({ |item| item.id.notNil })
					.collect({ |item| item.id.asString }); )
			.font_( Font( "Monaco", 9 ) )
			.action_({ |popUp|	
				var selectedID;
				if( popUp.value != 0 )
					{ 	
					selectedID = popUp.items[ popUp.value ].interpret;
					popUp.value = 0;
					WFSScoreEditor.all.select({ |score| score.id == selectedID })
						[0].toFront.makeCurrent;
					window.window.front; };
				});
			
		SCStaticText( window.window, Rect( window.window.bounds.width - 140, 80, 40, 16 ) )
			.string_( "score:" )
			.font_( Font( "Monaco", 9 ) ); 
		
		window.stopAction = { |tw|
			OSCresponder.remove( resp );
			tw.playRoutine.stop;
			WFSScore.stop;
			advancePosRoutine.stop;
			WFSClickTrack.stop;
			WFSTransport.videoAddr.sendMsg( "/video", "stop" );
			//{ tw.name = "WFSTransport" }.defer;
			{ loadViews[ \text ].string_( "" ); loadViews[ \wait ].stop; }.defer;
			
			
			
			};
		}
	}
	


