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

WFSEQ {

	classvar <window, <>action, <controls;
	classvar <calc;
	classvar currentArgsDict;
	
	*initClass {
		calc = ();
		
		calc[ 'gain2slider' ] = { |evt, in| in.round(0.25).linlin( -24, 24, 0, 1 )};
		calc[ 'slider2gain' ] = { |evt, in| in.linlin( 0, 1, -24, 24 ).round(0.25) };
		calc[ 'lowFr2slider' ] = { |evt, in| in.explin( 50, 1000, 0, 1 ) };
		calc[ 'slider2lowFr'] = { |evt, in| in.linexp( 0, 1, 50, 1000 ) };
		calc[ 'midFr2slider' ] = { |evt, in| in.explin( 500/3, 6000, 0, 1 ) };
		calc[ 'slider2midFr'] = { |evt, in| in.linexp(0,1,500/3,6000) };
		calc[ 'midRQ2slider' ] = { |evt, in| 1 - in.explin(0.1,10,0,1) }; 
		calc[ 'slider2midRQ' ] = { |evt, in| in.linexp(0,1,10,0.1) };
		calc[ 'highFr2slider' ] = { |evt, in| in.explin( 1000, 10289, 0, 1 ) };
		calc[ 'slider2highFr'] = { |evt, in| in.linexp(0,1,1000, 10289) };
		
		}

	*unit { |in| // 3 eq blocks in series with internal creation of control names
	
		// use as audiorate unit generator
		// requires BEQSuite
		
		var eqLowFr = 100, eqLowGain = 0.0, 
			eqMidFr = 1000, eqMidRQ = 1.0, eqMidGain = 0.0,
			eqHighFr = 6000, eqHighGain = 0.0;
		var eqOut;
		
		#eqLowFr, eqLowGain, eqMidFr, eqMidRQ, eqMidGain, eqHighFr, eqHighGain =
		[\eqLowFr, \eqLowGain, \eqMidFr, \eqMidRQ, \eqMidGain, \eqHighFr, \eqHighGain]
			.collect({ |name,i|
			Control.names([ name ]).kr( [ [100, 0.0, 1000, 1.0, 0.0, 6000, 0.0 ][i] ] );
			});
	
		eqOut = BLowShelf.ar( in, eqLowFr, 1, eqLowGain);
		eqOut = BPeakEQ.ar( eqOut, eqMidFr, eqMidRQ, eqMidGain );
		^BHiShelf.ar( eqOut, eqHighFr, 1, eqHighGain );
		}
		
	*new { if( window.notNil && { window.dataptr.notNil } )
			{ window.front; }
			{ this.newWindow;  };
		}
		
	*currentArgsDict {	^currentArgsDict ?? { currentArgsDict = this.asArgsDict; } }
	
	*updateCurrentArgsDict { currentArgsDict = this.asArgsDict; }
		
	*asArgsDict {
		var out = ();
		controls = controls ? ();
		
		[\eqLowGain, \eqMidGain, \eqHighGain].do({ |item, i| 
			out[ item ] = calc.slider2gain(controls[item].value ? 0.5);
			});
		
		out[ \eqMidRQ ] = calc.slider2midRQ(controls[ \eqMidRQ ].value ? 0.5);
		
		out[ \eqLowFr ] = calc.slider2lowFr(controls[ \eqLowFr ].value ? 
			calc.lowFr2slider( 100 ) );
		out[ \eqMidFr ] = calc.slider2midFr(controls[ \eqMidFr ].value ? 0.5);
		out[ \eqHighFr ] = calc.slider2highFr(controls[ \eqHighFr ].value ? 
			calc.highFr2slider( 6000 ) );
		
		^out;
		}
		
	*asArgsArray { ^this.asArgsDict.asArgsArray }
	
	*fromArgsDict { |argsDict, doAction = true| 
		var current;
		this.new;
		current = this.asArgsDict;
		argsDict = argsDict ? ();
		
		[\eqLowGain, \eqMidGain, \eqHighGain].do({ |item, i| 
			controls[ item ].value = calc.gain2slider(argsDict[item] ? current[ item ]);
			});
		
		controls[ \eqMidRQ ].value = calc.midRQ2slider(argsDict[ \eqMidRQ ] ? 
			current[ \eqMidRQ ] );
		
		controls[ \eqLowFr ].value = calc.lowFr2slider(argsDict[ \eqLowFr ].value ? 
			current[ \eqLowFr ] );
		controls[ \eqMidFr ].value = calc.midFr2slider(argsDict[ \eqMidFr ].value ? 
			current[ \eqMidFr ]);
		controls[ \eqHighFr ].value = calc.highFr2slider(argsDict[ \eqHighFr ].value ? 
			current[ \eqHighFr ] );

		if( doAction ) { this.doAction; };
		}
	
	*fromArgsArray { |argsArray, doAction = true| 
		this.fromArgsDict( argsArray.asArgsDict );
		}
	
	*doAction { 
		[\eqLowFr, \eqLowGain, \eqMidFr, \eqMidRQ, \eqMidGain, \eqHighFr, \eqHighGain].do({ |item|
			controls !? { controls[ item ] !? controls[ item ].doAction };
			});
		}

	*set { |paramName, value, doAction = true|
		var argsDict = ();
		argsDict[ paramName ] = value;
		this.fromArgsDict( argsDict, doAction );
	  }
	  
	 *get { |paramName|
	 	^this.asArgsDict[ paramName ];
	 	}
	 	
	 *reset { |doAction = true| 
	 	this.fromArgsDict( ( 
	 		eqLowGain: 0, eqMidGain: 0, eqHighGain: 0,
	 		eqLowFr: 100, eqMidFr: 1000, eqHighFr: 6000,
	 		eqMidRQ: 1.0 ), doAction );
	 	}
			
	*newWindow {
	
		var eqAction;
		
		controls = ();
		
		window = SCWindow( "WFS Equalizer", Rect(10, 394, 270, 292), false ).front;
		
		eqAction = { |paramName, value = 0, longParamName = "", format = ""|
			controls[ \eqTxt ].string = "Status: " ++ longParamName ++ 
				": " ++ value.round(0.001) ++ format;
			currentArgsDict = this.currentArgsDict;
			currentArgsDict[ paramName ] = value;
			action.value( this, paramName, value, longParamName, format );
			 };
		
		controls[ \eqButtons ] = [
			RoundButton( window, Rect( 5, 60, 50, 15 ) ).states_( [[ "reset", 
				Color.black, Color.gray(0.8).blend( Color.yellow, 0.1 ) ]] ),
			RoundButton( window, Rect( 5, 80, 50, 15 ) ).states_( [[ "save", 
				Color.black, Color.gray(0.8).blend( Color.red, 0.1 ) ]] ),
		 	RoundButton( window, Rect( 5, 100, 50, 15 ) ).states_( [[ "restore", 
		 		Color.black, Color.gray(0.8).blend( Color.green, 0.1 ) ]] ) 
		 	];
		
		controls[ \eqButtons ].do({ |bt| bt.canFocus_( false ).extrude_( false ); });
		
		controls[ \eqButtons ][0].action_({ |bt| 
			this.reset;
			controls[ \eqTxt ].string_( "Status: reset" ); 
			});
			
		controls[ \eqButtons ][1].action_({ |bt|
			var file;
			file = File( "wfs-eq-prefs.txt", "w" );
			file.write( this.asArgsDict.asString );
			file.close;
			controls[ \eqTxt ].string_( "Status: saved" ); 
			});
			
		controls[ \eqButtons ][2].action_({ |bt|
			var file, argsDict;
			file = File( "wfs-eq-prefs.txt", "r" );
			if( file.isOpen )
				{ argsDict = file.readAllString.interpret; file.close;
					this.fromArgsDict( argsDict );
				controls[ \eqTxt ].string_( "Status: restored" ); 
				} { "WFS Equalizer: prefs file not found".postln; }
			});
		
		controls[ \eqTxt ] = SCStaticText( window, Rect( 10, 0, 300, 20 ) )
			.string_( "Status: started" );
		
		LineView( window, Rect( 35 + 30, 129, 155, 1 ) ).full_(false);
		
		LineView( window, Rect(  65, 240, 1, 25 ) ).full_(false);
		SCStaticText( window, Rect( 35, 265, 60, 15 ) ).string_( "100Hz" ).align_( \center );
		
		LineView( window, Rect(  142, 255, 1, 10 ) ).full_(false);
		SCStaticText( window, Rect( 112, 265, 60, 15 ) ).string_( "1kHz" ).align_( \center );
		
		LineView( window, Rect(  219, 240, 1, 25 ) ).full_(false);
		SCStaticText( window, Rect( 189, 265, 60, 15 ) ).string_( "6kHz" ).align_( \center );
		
		controls[ \eqLowGain ] = SmoothSlider( window, Rect(35 + 30, 30, 30, 200 ) )
			.centered_( true ).value_( 0.5 )
			.canFocus_( false ).mode_( \move )
			.action_( { |sl| eqAction.value( \eqLowGain, 
				calc.slider2gain( sl.value ), "Low Shelf gain", "dB" )
				 } );
		
		controls[ \eqLowFr ] = SmoothSlider( window, Rect(10 + 30, 235, 100, 10 ) )
			.centered_( false ).value_( 100.explin( 50, 1000, 0, 1 ) )
			.hilightColor_( Color.clear ).knobSize_( 1 )
			.canFocus_( false ).action_( { |sl| eqAction.value( \eqLowFr, 
				calc.slider2lowFr( sl.value ), "Low Shelf freq", "Hz" )
				 } );
		
		SCStaticText( window, Rect( 35, 30, 28, 15 ) ).string_( "+24" ).align_( \right );
		SCStaticText( window, Rect( 35, 122, 28, 15 ) ).string_( "0" ).align_( \right ); 
		SCStaticText( window, Rect( 35, 215, 28, 15 ) ).string_( "-24" ).align_( \right ); 
			
		SCStaticText( window, Rect( 35 + 30, 15, 30, 15 ) ).string_( "low" ).align_( \center ); 
		
		
		controls[ \eqMidGain ] = SmoothSlider( window, Rect(97 + 30, 30, 30, 200 ) )
			.centered_( true ).value_( 0.5 )
			.canFocus_( false ).mode_( \move )
			.action_( { |sl| eqAction.value( \eqMidGain, 
				calc.slider2gain( sl.value ), "Mid Peak gain", "dB" )
				 } );
		
		controls[ \eqMidFr ] = SmoothSlider( window, Rect(45 + 25, 250, 145, 10 ) )
			.centered_( false ).value_( 0.5 )
			.hilightColor_( Color.clear ).knobSize_( 1 )
			.canFocus_( false ).action_( { |sl| eqAction.value( \eqMidFr, 
				calc.slider2midFr( sl.value ), "Mid Peak freq", "Hz" )
				 } );
		
		controls[ \eqMidRQ ] = SmoothSlider( window, Rect(132 + 30, 60, 10, 140 ) )
			.centered_( true ).value_( 0.5 ).knobSize_( 1 )
			.canFocus_( false ).mode_( \move ).action_( { |sl| eqAction.value( \eqMidRQ, 
				calc.slider2midRQ( sl.value ), "Mid Peak RQ", "" )
				 } );
		
		SCStaticText( window, Rect( 97 + 30, 15, 30, 15 ) ).string_( "mid" ).align_( \center );
		
		SCStaticText( window, Rect( 132 + 30, 45, 10, 15 ) ).string_( "Q" ).align_( \center );  
		
		controls[ \eqHighGain ] = SmoothSlider( window, Rect(160 + 30, 30, 30, 200 ) )
			.centered_( true ).value_( 0.5 )
			.canFocus_( false ).mode_( \move )
			.action_( { |sl| eqAction.value( \eqHighGain, 
				calc.slider2gain( sl.value ), "High Shelf gain", "dB" )
				 } );
			
		controls[ \eqHighFr ] = SmoothSlider( window, Rect(115 + 30, 235, 100, 10 ) )
			.centered_( false ).value_( 6000.explin(1000, 10289, 0, 1 ) )
			.hilightColor_( Color.clear ).knobSize_( 1 )
			.canFocus_( false )
			.action_( { |sl| eqAction.value( \eqHighFr, 
				calc.slider2highFr( sl.value ), "High Shelf freq", "Hz" )
				 } );
		
		SCStaticText( window, Rect( 160 + 30, 15, 30, 15 ) ).string_( "high" ).align_( \center ); 
		
		controls[ \eqButtons ][2].doAction;
		
		controls[ \eqTxt ].string_( "Status:" ); 
		}
		
	
	}
	