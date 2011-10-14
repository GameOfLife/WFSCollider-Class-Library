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

WFSDrawWindow {
	var window;
	var tmppoints, tmptimes, selected = 0, startTime = 0, scale = 10;
	
	*new { |bounds|	
		bounds = bounds ? Rect(128, 64, 340, 360);
		^super.new.initWindow( bounds );
		}
		
	currentPath { 
		 	if( tmppoints.size > 1 )
				{ ^WFSPath( 
					((tmppoints * (2 / window.bounds.extent.asArray.maxItem)) - 1 )
							.collect( _ * Point(1,-1) ), name: "Drawn_*")
						.timeLine_( tmptimes ).scale( scale ); };
		}
	
	initWindow { |bounds|
		var view;
		tmppoints = [];
		tmptimes = [];
		window = SCWindow("draw WFSPath", Rect(128, 64, 340, 360));
		window.view.background_( Color.black );
		view = SCUserView(window,Rect(0, 0, 340, 360))
			.mouseDownAction_({|v,x,y,z|
				
				//z.postln;
				case { ( (z != 131332) and: (z != 65792) ) and: (z != 524576) } 
					// no shift, capslock, alt
					{ tmppoints = []; tmptimes = []; startTime = Process.elapsedTime;
						selected = 0; }
					{ z == 524576 }  // alt
					{ startTime = Process.elapsedTime - tmptimes.last };
								
				tmppoints = tmppoints.add(x@y);
				tmptimes = tmptimes.add( Process.elapsedTime - startTime );
				window.refresh;
				})
			.mouseMoveAction_({|v,x,y|
				tmppoints = tmppoints.add(x@y);
				tmptimes = tmptimes.add( Process.elapsedTime - startTime );
				window.refresh;
				});
				
		SCButton(window, Rect(5,5,20,20) )
			.states_( [[ ">", Color.white,Color.white.alpha_(0.25)]] )
			.action_({ 
				var tmpDeltaTimes, lastTime = 0;
				tmpDeltaTimes = tmptimes;
				tmpDeltaTimes = tmpDeltaTimes.collect({ |item| var out;
					out = item - lastTime;
					lastTime = item;
					out;
					});
				selected = 0;
				Routine({ tmpDeltaTimes[1..].do({ |item|
							item.wait;
							selected = selected + 1;
							{ window.refresh; }.defer
							}); 
						//selected = 0;
						}).play;
				}); 
		
		SCButton(window, Rect(30,5,20,20) )
			.states_( [[ "x", Color.white, Color.red.alpha_(0.25)]] )
			.action_({ selected = 0; tmppoints = []; tmptimes = []; window.refresh; });
			
		SCButton(window, Rect(55,5,40,20) )
			.states_( [[ "Edit", Color.white, Color.green.alpha_(0.25)]] )
			.action_({ 	
				if( tmppoints.size > 1 )
					{ this.currentPath.edit;
						window.front; };
				/*
				{ WFSPath( 
					((tmppoints * (2 / window.bounds.extent.asArray.maxItem)) - 1 )
							.collect( _ * Point(1,-1) ), name: "Drawn_*")
						.timeLine_( tmptimes ).edit; }; */
			});
		
		SCNumberBox( window, Rect( 5, window.bounds.height - 25, 40, 20 ) )
			.background_( Color.black.alpha_(0.2) )
			.stringColor_( Color.white )
			.resize_( 7 )
			.value_( scale )
			.action_( { |box|
				if( box.value < 1 ) { box.value = 1 };
				scale = box.value;
				window.refresh;
				} );
				
		
		window.drawHook_{
					if( view.bounds != window.bounds.extent.asRect )
						{ view.bounds = window.bounds.extent.asRect };
					
					/* WFSConfiguration.default.plotSmoothInput( 
						[window.view.bounds.width, window.view.bounds.height].minItem,
						fromRect: Rect.aboutPoint( 0@0, 
							(scale / 2) / 1.05, 
							(scale / 2) / 1.05) ); */
					
					Pen.use {
						Color.red.set;
						Pen.addArc( tmppoints[selected] ? ((-10)@(-10)), 3, 0, 2pi ); 
						Pen.stroke;
						
						Color.white.set;
						Pen.width = 1;
						tmppoints.do{|p, i|if(i == 0)
							{ Pen.moveTo(p); } { Pen.lineTo(p); } };
						Pen.stroke;
					};
					
					
			
			};
		window.front
		}
		
	}