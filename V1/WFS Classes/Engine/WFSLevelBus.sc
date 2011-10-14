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

WFSLevelBus {
	
	classvar <id = 1;
	classvar <defaultLevel = -12; // in dB - reload SynthDefs after changing this
	classvar <level = -12;
	classvar <window, <slider, <nbox, <>range = 48; 
		// range: defaultLevel - range -> defaultLevel + range
	
	*kr { var busValue;
		busValue = In.kr( id, 1 );
		^this.rawToAmp( busValue ); 
	 	}
	 	
	 *makeWindow {
	 	var action;
	 	
	 	action = { |value| WFSLevelBus.set( value, false ); };
	 	
	 	if( window.notNil && { window.dataptr.notNil } )
	 		{ window.close };
	 	
	 	window = SCWindow( "level", Rect( SCWindow.screenBounds.width - 120, 0, 120, 
			SCWindow.screenBounds.height - 150 ), false ).front;
	
		slider = SmoothSlider( window, 
				window.view.bounds.resizeBy( -30, -100 ).moveBy( 15, 15 ))
			.mode_( \move ).canFocus_( false );
			
		nbox = ScrollingNBox( window, window.view.bounds
				.resizeTo( 90, 60 )
				.moveBy( 15, window.view.bounds.height - 80 ) )
			.value_( level )
			.font_( Font( "Monaco", 35 ) );
			
		nbox.action_( { |v| v.value = v.value
					.min( defaultLevel + range )
					.max( defaultLevel - range );
				slider.value = (v.value + ( defaultLevel - range ).neg) / (range * 2 );
				action.value( v.value );
				});
		
		slider.action_({ |v| 
			nbox.value = v.value.range(  
					defaultLevel - range , 								defaultLevel + range  ).round(1);
			action.value( v.value.range( defaultLevel - range, defaultLevel + range ) );
			});
				
		this.get( { |value|{  
			nbox.value = value.round(1);
			slider.value = ( value + ( defaultLevel - range ).neg ) / ( range * 2 );  }.defer });
	 	}
	 	
	*updateWindow {
		if( window.notNil && { window.dataptr.notNil } )
	 		{ WFSLevelBus.get( { |value| {  
				nbox.value = value.round(1);
				slider.value =  ( value + ( defaultLevel - range ).neg ) 
						/ ( range * 2 );  }.defer }); };
		}
	 
	*setRaw { |value = 0, servers, updateWindow = true|
		servers = servers ? WFSServers.default.allServers;
		servers.asCollection.do({ |server|
			Bus( \control, id, 1, server ).set( value ) });
		if( updateWindow ) { this.updateWindow };
		}
		
	*getAllRaw {  |action, servers|
		servers = servers ? WFSServers.default.allServers;
		action = action ? { |value, server, i| (server.name ++ " : " ++ value).postln; };
		servers.asCollection.do({ |server, i|
			Bus( \control, id, 1, server )
				.get( { |value| action.value( value, server, i ) } );
			});
		}
		
	*getRaw {  |action, servers|
		servers = (servers ? WFSServers.default.allServers).asCollection;
		this.getAllRaw( action, servers[0] );
		}
		
	*getAll { |action, servers|
		action = action ? { |value, server, i| (server.name ++ " : " ++ value).postln; };
		this.getAllRaw( { |value, server, i|
			action.value( this.rawToDB( value ), server, i ) } );
		}
		
	*get { |action, servers, use=true|
		action = action ? { |value, server, i| (server.name ++ " : " ++ value).postln; };
		this.getRaw( { |value, server, i|
			level = this.rawToDB( value );
			action.value( this.rawToDB( value ), server, i ) } );
		}
		
	*getAmp { |action, servers, use=true|
		action = action ? { |value, server, i| (server.name ++ " : " ++ value).postln; };
		this.getRaw( { |value, server, i|
			level = this.rawToDB( value );
			action.value( this.rawToAmp( value ), server, i ) } );
		}
		
	*set { |newLevel = -12, updateWindow = true| level = newLevel; 
			this.setCurrent( updateWindow ) ; }
			
	*setCurrent { |updateWindow = true| this.setRaw( this.dbToRaw( level ), 
				updateWindow: updateWindow ); }
	
	*check { |actionIfWrong|
		actionIfWrong = actionIfWrong ? { |value, server, i|
			 "WFSLevelBus-check: wrong buslevel for server '%': %\n"
			 	.postf( server.name, value ) };
		
		this.getAllRaw( 
			{ |value, server, i|
				if( this.rawToDB( value ) != level )
					{ actionIfWrong.value( this.rawToDB( value ) )   };
				});
		}
	
		
	*rawToAmp { |raw = 0| ^(raw + 1) * defaultLevel.dbamp; } // raw = 0 -> defaultLevel
	*ampToRaw { |amp = 1| ^(amp / defaultLevel.dbamp) - 1; }
	
	*rawToDB { |raw = 0| ^this.rawToAmp( raw ).ampdb; }
	*dbToRaw { |db| db = db ? defaultLevel; ^this.ampToRaw( db.dbamp ); }
	
	
	}
	
