/*
    GameOfLife WFSCollider - Wave Field Synthesis spatialization for SuperCollider.
    The Game Of Life Foundation. http://gameoflife.nl
    Copyright 2006-2011 Miguel Negrao, Wouter Snoei.

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

WFSPathEditor2 {
	
	classvar <>global;
	
	var <path;
	var <window;
	var <xyView;
	var <timeView;
	var <views;
	var <font;
	var <selected, <allSelected = false;
	
	var <settings;
	var <pos, <playTask;
	
	var <transformFuncs, <transformViewCreators, <transformViewSetters, <transformOrder;
	
	var <undoHistory, <>maxUndo = 50, <>undoStep = 0, changed = false;
	
	var <recordLastTime;
	
	var <selectRect, <hitIndex;
	var <hitPoint, <optionOn = false;
	
	var <tempPos, <tempTimes;
	
	var <>drawMode = 0; // 0: points+lines, 1: lines, 2: points, 3: none
	var <>showControls = false;
	
	var <mouseMode = \select; // \select, \move, \zoom
	
	*new { |path, width = 400, height = 500, global = true|
		^super.newCopyArgs( path ).init( width, height ).makeGlobal( global );
	}
	makeGlobal { |bool| if( bool ) { global = this } }
		
	*doesNotUnderstand { |selector ... args| // redirect to global
		if( global.respondsTo( selector ) )
			 { ^global.perform( selector, *args ) }
			 { ^super.doesNotUnderstand( selector, *args ) };
	}
	
	
//// SETTINGS ////////////////////////////////////////
	
	defaultSettings { ^(
			\name: path.name,
			\mode: settings !? { settings[ \mode ] } ? \cartesian,  // or polar 
			\move: 0@0, 
			\scale: 1@1, 
			\rotate:0, 
			\smooth: (
				\amount: 0,
				\order: settings !? { settings[ \smooth ][ \order ] } ? 10,
				
				// hidden features:
				\type: settings !? { settings[ \smooth ][ \type ] } ? \windowed,  
											// \median, \mean, \windowed
				\mode: settings !? { settings[ \smooth ][ \mode ] } ? \clip ), // \clip, \wrap, \fold
			\size: (
				\size: path.positions.size, 
				\type: settings !? { settings[ \size ][ \type ] } ? 'hermite', 					 // repeat, extend [ ... interpolate funcs ]
				\loop: false, 
				\extra: nil),
			\duration: (
				\timeScale: 1
			),
			\equal: (
				\mode: settings !? { settings[ \equal ][ \mode ] } ? \times,   // times, speeds
				\amount: 0
			),
			\morph: ( 
				\amount: 0, 
				\to: \circle ),
			\plot: (
				\mode: settings !? { settings[ \plot ][ \mode ] } ? \absolute, 
					// absolute, relative
				\active: settings !? { settings[ \plot ][ \active ] } ? true,
				\time: settings !? { settings[ \plot ][ \time ] } ? \speed // time, speed
			)
			); 
			}
			

		 
	init { |width, height|
		
		//path = path ?? { WFSPath.circle( 10, 0, 9 ) }; // default path
		
		path = path ?? { WFSPath2( { (8.0@8.0).rand2 } ! 7, [0.5] ); }; // default path
		path.intType = \bspline;
		
		//// defaults
		settings = this.defaultSettings;
		
		this.addToUndoHistory( path, \init );
		
		views = ();
		
		font = Font( "Helvetica", 10 );
		
			
//// WINDOW  ////////////////////////////////////////
		
		window = Window( "WFSPathEditor", Rect( 128, 64, width+170, height+20 ) ).front;
		
		views[ \header ] = CompositeView( window, Rect(0,0,width-20,20) );
		views[ \header ].decorator = FlowLayout( views[ \header ].bounds, 2@2, 2@2 );
		
		views[ \menu ] = PopUpMenu( views[ \header ], 60@16 )
			.items_( ["(File" /*)*/, "-", "save", "open", "-", "import SVG",
				 "export SVG", "export EPS" ] )
			.font_( font )
			.canFocus_( false )
			.action_({ |pu| pu.value = 0; "not implemented yet".postln; });
			
			
//// WINDOW HEADER ////////////////////////////////////////
			
		{ var selectRectIcon, zoomIcon, moveIcon;
			var icons;
			
			// TODO: move these to DrawIcon (?)
			icons = [	 
				{ |bt, rect|  // \select
						Pen.width = 1;
						Pen.lineDash_( FloatArray[ 1,1 ] );
						Pen.addRect( rect.insetBy( rect.width * 0.25,  rect.height * 0.25 ) );
						Pen.stroke;
				},  
				
				{ |bt, rect|  // \move
						Pen.width = 0.8;
						4.do({ |i|
							Pen.arrow( rect.center, 
								rect.center + Polar( rect.height * 0.35, i * 0.5pi ).asPoint, 
								rect.height / 6 );
						});
						Pen.stroke;
				},  { |bt, rect|  // \zoom
						Pen.width = 1;
						rect = rect.insetBy( rect.width * 0.25,  rect.height * 0.25 );
						Pen.addOval(
							rect.insetBy( rect.width * 0.125,  rect.height * 0.125 )
						 		.moveBy(  rect.width * 0.25,  rect.height * -0.25 ) 
						 	);
						Pen.line( rect.center, rect.leftBottom );
						Pen.stroke;
			},
			 { |bt, rect|  // \record
						Pen.color = Color.red(0.75);
						DrawIcon( \record, rect );
						Pen.fill;
			}
			];
	
			views[ \tools ] = [ \select, \move, \zoom, \record ].collect({ |item, i|
					RoundButton( views[ \header ], 16@16 )
						.radius_(2).border_(1).extrude_(false).canFocus_(false)
						.states_([					 						[ icons[i], Color.black ],
							[ icons[i], Color.white, Color.gray(0.4) ] 
							])
						.action_({ |bt|
							views[ \tools ].do( _.value_(0) );
							bt.value = 1;
							if( item === \zoom && { mouseMode === \zoom } )
								{ this.fit; views[ \tools ][0].valueAction = 1; }
								{ mouseMode = item; }
							});
					}); 
			views[ \tools ][ 0 ].value = 1;
		}.value;
			
		views[ \play ] = RoundButton( views[ \header ], 16@16 )
				.radius_(2).border_(1).extrude_(false).canFocus_(false)
				.states_([					 						[ 'play', Color.green(0.5) ],
					[ 'play', Color.green,  Color.gray(0.4) ] 
					])
				.action_({ |bt|
					var res = 0.05;
					switch( bt.value,
						1, { playTask = Task({	
								while { views[ \play_bar ].value < 1 }
									{  views[ \play_bar ].valueAction = 
									   views[ \play_bar ].value + (0.05 / path.length);
									   0.05.wait;
									};
								views[ \play_bar ].value = 0;
								bt.value = 0;
								pos = nil;
								{ xyView.refresh; }.defer;
							}).start;
							}, 
						0,  { playTask.stop; 
							views[ \play_bar ].value = 0;
							pos = nil; 
							xyView.refresh; });
				});
					
		
		views[ \play_bar ] = RoundSlider( views[ \header ], 60@16 )
				.knobSize_(0).thumbSize_(0).border_(1)
				.extrude_(false).borderColor_(Color.gray(0.25) )
				.baseWidth_(0.4).canFocus_(false)
				.hilightColor_( Color.gray(0.2).alpha_(0.5) )
				.action_({ |sl|
					pos = sl.value.linlin(0,1, 0, path.length );
					{ xyView.refresh; timeView.refresh; }.defer;
				})
				.mouseUpAction_({ |sl|
					if( views[\play].value != 1 )
						{ pos = nil; sl.value = 0; xyView.refresh; timeView.refresh; };
				});
		
		views[ \draw_mode ] = RoundButton( views[ \header ], 16@16 )
						.radius_(2).border_(1).extrude_(false).canFocus_(false)
						.states_({ |i|
							[ { |bt, rect| // drawMode
								rect = rect.insetBy( 3,3 );
								if([0,1].includes( i ) )
									 { Pen.color = Color.gray(0.25);
									   Pen.width = 0.5;
									   Pen.line( rect.leftTop, rect.rightBottom ).stroke;
									 };
								if([0,2].includes( i ) )
									 { Pen.width = 1;
									   Pen.color = Color.blue(1,0.5);
									   Pen.addOval( 
									   		Rect.aboutPoint( rect.center, 2.5, 2.5 ) 
									   	).stroke;
									 };
								} ]
							}!4)
						.action_({ |bt| 
							drawMode = bt.value;
							xyView.refresh;
						});
		
		views[ \undo ] = { |i|
			RoundButton( views[ \header ], 16@16 )
						.radius_(2).border_(1).extrude_(false)
							.canFocus_(false).enabled_( false )
						.states_([ [ ['arrow_pi', 'arrow'][i] ] ])
						.action_({ this.undo( [-1,1][i] ) })

		}!2;
					
		views[ \plot_comp ] = CompositeView( window, Rect( 0, 0, width, height+100 ) ).resize_(5);
		views[ \edit_comp ] = CompositeView( window, Rect(width,20,170,height+20).insetBy(2,2) )
				.background_( Color.white.alpha_(0.1) )
				.resize_(6);
		views[ \edit_comp ].decorator = FlowLayout( views[ \edit_comp ].bounds );
		
		views[ \edit_switch ] = RoundButton( views[ \plot_comp ],
				 Rect( width-20, 0, 20, 20 ).insetBy( 2,2 ) ).resize_( 3 )
			.states_( [ [ 'down' ], [ 'play' ] ] )
			.border_(0)
			.canFocus_(false)
			.value_(1)
			.action_({ |bt|
				switch( bt.value,
					1, { 
						views[ \plot_comp ].bounds = 
							views[ \plot_comp ].bounds.width_(
								views[ \plot_comp ].bounds.width - 170 );						views[ \edit_comp ].bounds = 
							views[ \edit_comp ].bounds.left_( 
								views[ \edit_comp ].bounds.left - 170 );
						//window.bounds = window.bounds.width_( window.bounds.width + 170 );
						},
					0, { 
						views[ \plot_comp ].bounds = 
							views[ \plot_comp ].bounds.width_(
								views[ \plot_comp ].bounds.width + 170 );
						views[ \edit_comp ].bounds = 
							views[ \edit_comp ].bounds.left_( 
								views[ \edit_comp ].bounds.left + 170 );
						//window.bounds = window.bounds.width_( window.bounds.width - 170 );
						});
			});
		
		
//// MAIN PLOT VIEWS ////////////////////////////////////////
		
		xyView = ScaledUserView.withSliders( views[ \plot_comp ], 
				Rect( 0,20,width,height-50 ).insetBy(2,2), 
				Rect(-100, -100, 200, 200 ) )
			.scaleSliderLength_( 40 )
			.sliderWidth_( 10 )
			.move_( [0.5,0.5] )
			.scale_( [10,10] )
			.maxZoom_( 20 )
			.keepRatio_( true )
			.resize_(5)
			.gridLines_( [ 200, 200 ] )
			.gridMode_( \lines );
		
		timeView = ScaledUserView.withSliders(  views[ \plot_comp ], 
				Rect( 0,height-30,width,50 ).insetBy(2,2) )
			.scaleSliderLength_( 30 )
			.sliderWidth_( 10 )
			.moveVEnabled_( false ).scaleVEnabled_( false )
			.resize_( 8 );
	
		xyView.gridColor = Color.white.alpha_(0.25);
		
		xyView.mouseDownAction = { |vw, x,y, mod, oX, oY, isInside, bn, cc|
			var scaler, includes;
			scaler = vw.pixelScale.asArray.mean;
			mod = ModKey(mod);
			
			switch( mouseMode, 
				\select, {  
					this.finalize( \mouse_down );
					hitPoint = (x@y);
					hitIndex = path.positions
						.detectIndex({ |pt, i|
							pt.asPoint.scale(1@(-1)).dist( hitPoint ) <= (scaler * 5)
						});
					
					includes = selected.asCollection.includes( hitIndex );
					
					// a = hitPoint; // start dragging rect if nil
					
					if( mod.shift )
						{ if( hitIndex.notNil ) 
							{ if( includes ) 
								{ selected.remove( hitIndex ); this.select( selected ); }
								{ this.select( selected.add( hitIndex ) ); }; }; 
						}
						{ if( hitIndex.notNil ) 
							{ if( includes.not ) { this.select( hitIndex ) };
							} { this.select() };
						};
				//timeView.refresh;
				}, \move, {
					if( cc == 2 ) { vw.movePixels = [0,0]; };
					hitPoint = vw.movePixels.asPoint - (oX@oY);
				}, \zoom, {
					if( cc == 2 )
						{ this.fit; }
						{ hitPoint = (x@y); };
				}, \record, {
					this.finalize( \start_record );
					selected = [];
					this.updateGUI;
					this.startRecord( (x@y) * (1@ -1), true );
				});
				
			vw.refresh;
		};
		
		xyView.mouseMoveAction = { |vw, x,y, mod, oX, oY|
			var newPoint, pts, tms;
			mod = ModKey(mod);
			
			switch( mouseMode,
				\select, {	
					newPoint = (x@y);	
					
					// move canvas if out of bounds
					if( vw.viewRect.contains( newPoint ).not )
						{{ xyView.viewRect =   // change to moving/not scaling later?
							xyView.viewRect.union( 
									Rect.fromPoints( newPoint, newPoint ) )
								.sect( vw.fromBounds ); }.defer(0.5); // 0.1s delay 
						}
						{	
						if( hitIndex.isNil )
							{  // no point hit
							// create selectRect
							 selectRect = Rect.fromPoints( hitPoint, newPoint ).scale(1@(-1));
							 pts = [];
							 path.positions.do({ |pt, i|
								 if( selectRect.contains( pt.asPoint ) ) { pts = pts.add(i) };
							 });
							 
							 if( mod.shift ) // expand selection
							 	{this.select( 
								 	((selected ? []).asSet.addAll( pts ) ).asArray ); 
								}
							 	{ this.selectNoUpdate(pts); }; // replace selection
							 vw.refresh; 
							}
							{ // selected pont hit
							  // changing path
							if( mod.option ) // duplicate
								{ if( optionOn.not )
										{ 
										this.finalize( \duplicate );
										if( selected.size == 1 )
											{ // single item
											path.positions.insert(
												selected[0]+1,
												path.positions[selected[0]] );
											path.times.insert(
												selected[0],
												path.times[selected[0]] ?
												path.times[selected[0]-1] );
											selected = selected + 1;
											settings[ \size ][ \size ] = 
											settings[ \size ][ \size ] + selected.size;
																						} { // multiple items
											selected = selected.sort;
											pts = path.positions[ selected ];
											tms = path.times[ selected ];
											tms = tms.collect({ |item, i|
												item ?? { path.times[ selected[i]-1 ] };
											});
											path.positions = 
												path.positions[..selected.last] ++
												pts ++ 
												path.positions[selected.last+1..];
											path.forceTimes(
												path.times[..selected.last] ++
												tms ++ 
												path.times[selected.last+1..];
											);
											selected = (0..pts.size-1) + selected.last + 1;										};
										optionOn = true;
										};	
								views[ \edit ][ \size ][ \size ].valueAction 
									= path.positions.size;
								};											// move
							this.move( *(( newPoint - hitPoint ) * (1@(-1))).asArray 
								++ [false, false] );
							
							};
						};
				}, \move, { xyView.movePixels_( hitPoint + (oX@oY) ); 
				}, \zoom, {
						if( hitPoint.notNil )
							{ 
							newPoint = (x@y);	
							if( hitPoint.dist( newPoint ) > 1 )
								{ selectRect = Rect.fromPoints( hitPoint, newPoint )
										.scale(1@(-1)); }
								{ selectRect = nil };
							};
				}, \record, {
					this.recordPoint( (x@y) * (1@ -1) );
					xyView.refresh; timeView.refresh;
				});
		};
		
		xyView.mouseUpAction = { |vw, x, y, mod|
				
				if( mouseMode == \zoom )
					{ 
					 if( hitPoint.notNil )
					 	{ if( selectRect.notNil )
							{ xyView.viewRect = selectRect.scale(1@(-1)) }
							{ mod = ModKey( mod );
								case { mod.shift }
								 { xyView.scale = xyView.scale * 2.sqrt }
								 { mod.ctrl }
								 {  xyView.scale = xyView.scale / 2.sqrt };
							};
					 	} 
					};
					
				//if( changed ) { this.addToUndoHistory( path, \mouse_edit ); changed = false; };
				//tempPos = nil;
				optionOn = false;
				if( mouseMode == \record )
					{ this.endRecord; 
						 views[ \tools ][ 0 ].valueAction = 1;
						 views[ \edit ][ \size ][ \size ].valueAction = path.positions.size;
						  }
					{	
					this.finalize( \mouse_edit );
					selectRect = nil;
					hitPoint = nil;
					};
				vw.refresh;
		};
		
		xyView.keyDownAction = { |vw, char, modifiers, unicode, keycode|
			var dict;
			dict = (127: \backspace, 63234: \leftArrow, 63235: \rightArrow,
			 63232: \upArrow, 63233: \downArrow);
			 
			switch( dict[ unicode ],
				\backspace, { 
					this.finalize;
					
					path.positions = path.positions.select({ |item, i|
						selected.includes(i).not;
					});
					path.forceTimes( path.times.select({ |item, i|
						selected.includes(i).not;
					}) );
					selected = [];
					settings = this.defaultSettings; // correct "size"
					this.updateGUI;
				},
				\leftArrow, { 
					if( selected.size == 0 ) { this.select(\all) };
					this.move( settings[ \move ] + (-0.1 @ 0) )},
				\rightArrow, { 
					if( selected.size == 0 ) { this.select(\all) };
					this.move( settings[ \move ] + (0.1 @ 0) ) },
				\upArrow, { 
					if( selected.size == 0 ) { this.select(\all) };
					this.move( settings[ \move ] + (0 @ 0.1) ) },
				\downArrow, { 
					if( selected.size == 0 ) { this.select(\all) };
					this.move( settings[ \move ] + (0 @ -0.1) ) }
				);
		};
		
			
//// MAIN DRAWING FUNCTIONS ////////////////////////////////////////
		
		xyView.drawFunc = { |vw|
				
				var scaler;
				var selectColor = Color.yellow;
				var pospt, times;
				var points, controls;
				
				scaler = vw.pixelScale.asArray.mean;
			
				Pen.use({	 
					
					var curves;
					Pen.scale( 1, -1 );
					Pen.width = 0.164;
					
					
					//// configuration
					Pen.color = Color.red(0.5, 0.5);
					WFSConfiguration.default.speakerLines.do({ |spl|
						Pen.line( *spl.corners.collect(_.asPoint) );
					});
					Pen.stroke;
					
					//// path
					points = path.positions.collect(_.asPoint);
					controls = if( path.respondsTo( \controls ) )
							{ path.controls }
							{ points.collect({ |item, i|
									points.clipAt( (i-1..i+2) ).collect(_.asArray).flop
										.collect({ |array|
										array.splineIntControls(1/3)
									}).flop.collect( _.asPoint);
								});	
							};
					curves = points.collect({ |item,i| [ item ] ++ controls[i] });
					
					
					// controls
					
					if( showControls )
					{	Pen.color = Color.gray(0.35);
						Pen.width = scaler * 0.25;
						
						points[1..].do({ |item, i|
							Pen.line( points[i], controls[i][0] );
							Pen.line( item, controls[i][1] );
							Pen.stroke;
							Pen.addArc( controls[i][0], 1.5 * scaler, 0, 2pi );
							Pen.addArc( controls[i][1], 1.5 * scaler, 0, 2pi );
							Pen.fill;
						});
					};
					
					
					// lines
					
					if( [0,1].includes( drawMode ) )
					{	Pen.width = scaler * 0.5;
						Pen.color = Color.gray(0.25);
						Pen.moveTo( curves[0][0] );
						curves[1..].do({ |item, i|
							Pen.curveTo( item[0], *curves[i][1..] );
						});
						Pen.stroke;
					};
					
					// points
					if( drawMode != 3 )
					{	
						Pen.width = scaler;
						Pen.color = Color.green(0.5,0.5); // start point
						Pen.moveTo( curves[0][0] );
						Pen.addArc( curves[0][0], 4 * scaler, 0, 2pi );
						Pen.fill;
						
						Pen.color = Color.red(1, 0.5); // end point
						Pen.moveTo( curves.last[0] );
						Pen.addArc( curves.last[0], 4 * scaler, 0, 2pi );
						Pen.fill;
					};
					
					if( drawMode == 4 )
					{	times = path.times / path.times.mean;
						Pen.color = Color.gray(0.25);
						curves[1..].do({ |item, i|
							var n, pt1;
							n = times[i] * 10;
							pt1 = [ curves[i][0], item[0] ].splineIntPart1( *curves[i][1..] );
							n.do({ |ii|
								Pen.addArc( pt1.splineIntPart2( ii / n ), scaler, 0, 2pi );
							});
						});
						Pen.fill;
					};
					
					// selected
					Pen.use({	
						if( selected.notNil ) {	
							Pen.width = scaler * 2;
							Pen.color = selectColor;
							selected.do({ |item|
								Pen.moveTo( curves[item][0] );
								Pen.addArc( curves[item][0] , 4 * scaler, 0, 2pi );
							});
							
							Pen.fill;
						};
					});
					
					// show position
					if( pos.notNil )
						{
							pospt = path.atTime2( pos, 'hermite', loop: false ).asPoint;
							Pen.color = Color.black.alpha_(0.5);
							Pen.moveTo( pospt );
							Pen.addArc( pospt , 5 * scaler, 0, 2pi );
							Pen.fill;
						};
					
					if( [0,2,4].includes( drawMode ) )
					{	
						Pen.color = Color.blue(1,0.5);
						curves[1..curves.size-1].do({ |item|
							Pen.moveTo( item[0] );
							Pen.addArc( item[0], 3 * scaler, 0, 2pi );
						});
						Pen.stroke;
					};
				});
			};
			
		xyView.unscaledDrawFunc = { |vw|
				var rect;
				
				/// border
				if( vw.view.hasFocus ) { Pen.width = 3; } { Pen.width = 1 };
				Pen.color = Color.gray(0.2).alpha_(0.75);
				Pen.strokeRect( vw.drawBounds.insetBy(0.5,0.5) );
				
				//// selection
				if( selectRect.notNil )
					{ 
					Pen.width = 1;
					rect = selectRect.scale(1@(-1));
					rect = vw.translateScale(rect);
					switch( mouseMode,
							\select, {
							  //Pen.fillColor = selectColor.copy.alpha_(0.05); 
							  // Pen.strokeColor = selectColor.copy.alpha_(0.5); 
							  Pen.fillColor = Color.black.alpha_(0.05); 
							  Pen.strokeColor = Color.black.alpha_(0.25);
							  Pen.lineDash_( FloatArray[4, 4] );
							},
							\zoom, {
							  Pen.fillColor = Color.black.alpha_(0.05); 
							  Pen.strokeColor = Color.black.alpha_(0.25); 
							});
						
					Pen.addRect( rect ).fillStroke;
				};
		};
		
		timeView.fromBounds = Rect( 0, -0.5, 1, 1 );
		
		timeView.drawFunc = { |vw|
			var times, speeds, timesSum, meanSpeed;
			var scaler;
			var drawPoint;
			var selectColor = Color.yellow;
			var tempPath;
			var pospt;
			
			scaler = vw.pixelScale;
			
			drawPoint = { |point, r = 3, w = 1|
				Pen.addOval( 
					Rect.aboutPoint( point, scaler.x * r, scaler.y * r ) );
				Pen.addOval( 
					Rect.aboutPoint( point, scaler.x * (r-(w/2)) , scaler.y * (r-(w/2)) ) );
			};
			
			tempPath = path; //.copyNew.interpolate(10);

			if( tempPath.times.size > 0 )
			{	
				timesSum = tempPath.times.sum;
				times = ([ 0 ] ++ tempPath.times.integrate) / timesSum;
				
				
				speeds = tempPath.speeds;
				meanSpeed = (speeds * tempPath.times).sum / timesSum;
				speeds = speeds ++ [0];
				
				Pen.color = Color.blue(0.5).blend( Color.white, 0.5 );
				times.do({ |item, i|
					//Pen.color = Color.red(0.75).alpha_( (speeds[i] / 334).min(1) );
					Pen.addRect( 
						Rect( item, 0.5, times.clipAt(i+1) - item, speeds[i] / -344));
								
				});
				Pen.fill;	
							
				Pen.color = Color.gray(0.25); // line
				Pen.addRect(Rect( 0, 0 - (scaler.y/4), 1, scaler.y/2 ) ).fill;
	
				Pen.color = Color.green(0.5,0.5); // start point
				Pen.addOval( Rect.aboutPoint( times[0]@0, 
					scaler.x * 5, scaler.y * 5 ) );		
				Pen.fill;
					
				Pen.color = Color.red(1, 0.5); // end point
				Pen.addOval( Rect.aboutPoint( times.last@0, 
					scaler.x * 5, scaler.y * 5 ) );		
				Pen.fill;
				
				Pen.color = selectColor; // selected points
				selected.do({ |item| 
					Pen.addOval( Rect.aboutPoint( times[item]@0, 
						scaler.x * 3.5, scaler.y * 3.5 ) );
					});
				Pen.fill;
				
				if( pos.notNil )
						{
							pospt = pos / timesSum;
							Pen.color = Color.black.alpha_(0.5);
							Pen.width = scaler.x * 2;
							Pen.line( pospt @ -0.5, pospt @ 0.5 ).stroke;
						};
				
				Pen.color = Color.blue(0.5);
				times[1..].do({ |item, i| drawPoint.( item@0 ); });
				Pen.draw(1);
			};
		};
		
		timeView.unscaledDrawFunc = { |vw|
				// border
				if( vw.view.hasFocus ) { Pen.width = 3; } { Pen.width = 1 };
				Pen.color = Color.gray(0.2).alpha_(0.75);
				Pen.strokeRect( vw.drawBounds.insetBy(0.5,0.5) );
		};
		
		this.initTransform;
		this.createTransformViews;	
		
		this.createPlotViews;	
	}
	
	
//// TRANSFORMATION FUNCTIONS ////////////////////////////////////////

	initTransform { |order|
		var defaultSettings;
		defaultSettings = this.defaultSettings;
		// position transformations
		order = order ? [ \move, \scale, \rotate, \smooth, \size, \duration, \equal ]; 		transformOrder = order;
		
		transformFuncs = (
			\move: { |positions, times| 
					var polar;
					if( settings[ \mode ] == 'polar' )
					{	//polar = settings[ \move ].asPolar;
						polar = { |negCenter = (pi)|
							var rho, theta;
							rho = settings[ \move ].rho;
							theta = settings[ \move ].theta;
							if( theta.wrap(negCenter - pi, negCenter + pi)
									.inclusivelyBetween( negCenter - 0.5pi, 
										negCenter + 0.5pi) ) { 
								theta = theta.wrap(0,pi);
								rho = rho.neg;
							};
							Polar( rho, theta );
						}.value;
						[positions.collect({ |item,i|
							var itemPolar;
							if( selected.includes(i) ) 
								{ itemPolar = item.asPoint.asPolar;
								  Polar( 
								  		itemPolar.rho + polar.rho, 
								  		itemPolar.theta + polar.theta )
								  	.asPoint.asWFSPoint; } 
								{item} 
							}),
						times] 
					} {	
						[
						positions.collect({ |item, i|
							if( selected.includes(i) ) { item + settings[ \move ] } {item} }), 
						times
						]};
					 },			
			\scale: { |positions, times| 
					var polar;
					if( settings[ \mode ] == 'polar' )
					{
					polar = Polar( settings[ \scale ].x, settings[ \scale ].y );
									
					// unwrap:
					positions = positions.collect({ |item| var pl; 
							pl = item.asPoint.asPolar; 
							[ pl.rho, pl.theta ] 
						}).flop;
						
					positions[1] = positions[1].unwrap2(pi);
					positions = positions.flop.collect({ |item| Polar( *item ) });
					
					[
					positions.collect({ |item,i|
						
						if( selected.includes(i) ) 
							{  Polar( item.rho * polar.rho,
									 item.theta * polar.theta ).asPoint.asWFSPoint; } 
							{ item.asPoint.asWFSPoint }
							}),
					times
					]

					 }{
					[
					positions.collect({ |item,i|
						if( selected.includes(i) ) 
							{ item * settings[ \scale ] } { item } }),
					times
					]} },
					
			\rotate: { |positions, times| 
					[
					positions.collect({ |item, i|
						 if( selected.includes(i) ) { item.asPoint.rotate( 
							settings[ \rotate ].linlin(-180,180,-pi,pi,\none) ) } {item} }), 
					times
					]; },	
					
			\smooth: { |positions, times| 
					var win, n, pos, clipMode;
					n = settings[ \smooth ][ \order ];
					clipMode = if( path.respondsTo( \intClipMode ) ) 
							{ path.intClipMode } 
							{ settings[ \smooth ][ \mode ] };
					if( settings[ \smooth ][ \amount ] != 0 )
						{ if( [ \mean, \median ].includes( settings[ \smooth ][ \type ] ).not )
							{ win = ({ |i| 
									i.linlin(0,(n-1).max(2),-0.5pi,1.5pi).sin.linlin(-1,1,0,1) 
									}!n.max(2)).normalizeSum;
							};
						pos = switch( settings[ \smooth ][ \type ],
							\median, { 
								positions.collect({ |item, i|
									var out, sum;	
									out = positions.modeAt( 
											(i + (n/ -2).ceil .. i + (n/2).ceil - 1),
											clipMode ).collect(_.asArray)
										.flop.collect(_.median)
										.flop.collect(_.asPoint);
									sum = 0@0;
									out.do({ |item| sum = sum + item; });
									sum;
									});
								},
							\mean, {
								positions.collect({ |item, i|
									var out, sum;	
									out = positions.modeAt( 
										(i + (n/ -2).ceil .. i + (n/2).ceil - 1),
										clipMode ) * 1/n;
									sum = 0@0;
									out.do({ |item| sum = sum + item; });
									sum;
									});
								},
							{ // windowed (default)	
								positions.collect({ |item, i|
									var out, sum;	
									out = positions.modeAt( 
											(i + (n/ -2).ceil .. i + (n/2).ceil - 1), 
											clipMode ) * win;
									sum = 0@0;
									out.do({ |item| sum = sum + item; }); 
										// [ ].sum doesn't work..
									sum;
									}) 
								});
						pos = positions.collect({ |item, i| 
								item.blend( pos[i], settings[ \smooth ][ \amount ] ) });
						} { pos = positions };
					
					[ pos, times ]; },	
			\size: { |positions, times|
					var newPos, newTimes;
					if( positions.size != settings[ \size ][ \size ] )
					{
						
					if( settings[ \mode ] == 'polar' )
						{
					positions = positions.collect({ |item| var pl; 
							pl = item.asPoint.asPolar; 
							[ pl.rho, pl.theta ] 
						}).flop;
						
					positions[1] = positions[1].unwrap2(pi);
					positions = positions.flop.collect({ |item| Point( *item ) });
						};
					case { ['extend', 'prepend', 'repeat', 'fold' 
							].includes( settings[ \size ][ \type ] ) }
						{
								switch( settings[ \size ][ \type ],
									\repeat, {
										newPos = positions.wrapAt(
											 (..settings[ \size ][ \size ].asInt -1 ) );
										newTimes = (times ++ [ times.last ])
											.wrapAt((..settings[ \size ][ \size ].asInt -1 ))												[..settings[ \size ][ \size ].asInt -2];
									},
									\fold, {
										newPos = positions.foldAt(
											 (..settings[ \size ][ \size ].asInt -1 ) );
										newTimes = (times)
											.foldAt((..settings[ \size ][ \size ].asInt -1 ))												[..settings[ \size ][ \size ].asInt -2];
									},
									\extend, {
										newPos = 
				positions[..(settings[ \size ][ \size ].asInt-1).min(positions.size-1)];
										newTimes = 
				times[..(settings[ \size ][ \size ].asInt - 2).min(times.size-1) ];
										(settings[ \size ][ \size ] - newPos.size)
											.do({
											var points;
											var newPoint;
											points = 
												[ newPos.last.asPoint, 
												  newPos.wrapAt(-2).asPoint ];
											newPoint = (points[0] * 2) - points[1]; 
											newPos = newPos ++ [ newPoint.asWFSPoint ];
											
											newTimes = newTimes ++ [ newTimes.last ];

											});
									},
									\prepend, {
										newPos = 
				positions.reverse[..(settings[ \size ][ \size ].asInt-1).min(positions.size-1)]
					.reverse;
										newTimes = 
				times.reverse[..(settings[ \size ][ \size ].asInt - 2).min(times.size-1) ]
					.reverse;
										(settings[ \size ][ \size ] - newPos.size)
											.do({
											var points;
											var newPoint;
											points = [ newPos.first.asPoint,
												 newPos.wrapAt(1).asPoint ];
											newPoint = (points[0] * 2) - points[1]; 
											newPos = [ newPoint.asWFSPoint ] ++ newPos;
											newTimes =[ newTimes.first ] ++  newTimes;
											});
									}
									);
						}
						{	
						newPos = positions.collect(_.asArray).resize( 
							settings[ \size ][ \size ], 
							settings[ \size ][ \type ], 
							settings[ \size ][ \loop ], 
							settings[ \size ][ \extra ] ).collect(_.asWFSPoint);
						newTimes =  ([0] ++ times.integrate).resize( 
								settings[ \size ][ \size ], \linear, false )
							.differentiate[1..];
						};
					
					if( settings[ \mode ] == 'polar' )
						{ newPos = newPos.collect({ |item| 
							Polar( item.x, item.y ).asPoint.asWFSPoint }) };
					[newPos, newTimes]
					} { [ positions, times ] };
			},
			\duration: { |positions, times|
				[ positions, times * settings[ \duration ][ \timeScale ].max(64 / 44100) ]
			},
			\equal: { |positions, times|
				var deltas;
				switch( settings[ \equal ][ \mode ],
					\times, {
					[ positions, 
						times.blend( (1!(times.size)).normalizeSum * times.sum,
							 settings[ \equal ][ \amount ] ) ]
					},
					\speeds, {
					deltas = positions[1..].collect({ |pos, i| pos.dist( positions[i] ) });
					[ positions, 
						times.blend( deltas.normalizeSum * times.sum, 
							settings[ \equal ][ \amount ] ) ]
					});
					
					
			},		
			\morph: { |positions, times|	
			}
		);
		
//// TRANSFORMATION VIEW CREATORS ////////////////////////////////////////
		
		transformViewCreators = (
		
			\createLabel: { |parent, evt, name|
				evt[ \label ] = StaticText( parent, 50@16 )
					.font_( font ).align_( \right )
					.string_( name.asString );
			},
			
			\name: { |parent, evt|
				
				transformViewCreators[ \createLabel ].value( parent, evt, "name" );					
				evt[ \name ] = TextField(parent, 104@16).font_( font )
					.background_( Color.white.alpha_(0.5) )
					.action_({ |tf| 
						path.name = tf.string;
						this.doTransform( true );
						});
				},
			
			\interpolation: { |parent, evt|
				
				transformViewCreators[ \createLabel ].value( parent, evt, "interp." );
				
				evt[ \hideFunc ] = { { 
						if( evt[ \type ].value == 0 ) 
							{ evt[ \curve ].enabled = false }
							{ evt[ \curve ].enabled = true };
						/*
						if( evt[ \type ].value == 2 ) 
							{ evt[ \clip ].enabled = false }
							{  evt[ \clip ].enabled = true };
						*/
						 }.defer;
				};
				
				evt[ \type ] = PopUpMenu( parent, 60@16 )
					.canFocus_(false).font_(font)
					.items_([ 'bspline', 'cubic', 'linear' ])
					.action_({ |pu| 
						if( path.respondsTo( \intType ) )
							{ path.intType = pu.item;
							 evt[ \hideFunc ].value;
							 this.doTransform( true );
							};
						});
						
				
				evt[ \curve ] = SmoothNumberBox( parent, 40@16 )
					.value_( if( path.respondsTo( \curve ) ) { path.curve ? 1 } { 1 } )
					.step_(0.1)
					.scroll_step_(0.1)
					.action_({ |nb| 
						if( path.respondsTo( \curve ) )
							{ path.curve = nb.value;
							 this.doTransform( true );
							};
						});
						
						
				evt[ \type ].value = if( path.respondsTo( \intType ) )
					{ evt[\type].items.indexOf( path.intType ); } { 1 };
						
				StaticText( parent, 50@16 );
						
				evt[ \clip ] = PopUpMenu( parent, 104@16 )
					.canFocus_(false).font_(font)
					.items_([ 'clip', 'wrap', 'fold' ])
					.action_({ |pu| 
						if( path.respondsTo( \intClipMode ) )
							{ path.intClipMode = pu.item;
							 this.doTransform( true );
							};
						});
		
				evt[ \hideFunc ].value;
			},
			
			\mode: { |parent, evt| 
				
				transformViewCreators[ \createLabel ].value( parent, evt, "mode" );
				
				evt[ \mode ] = PopUpMenu( parent, 104@16 )
					.canFocus_(false).font_(font)
					.items_([ 'cartesian', 'polar' ])
					.action_({ |pu| 
						settings[ \mode ] = pu.items[ pu.value ];
						this.doTransform( true );
						});
				
			},
			\move: {	 |parent, evt| 
					
				// move
				transformViewCreators[ \createLabel ].value( parent, evt, "move" );
					
				evt[ \x ] = SmoothNumberBox( parent, 40@16 ).value_(0);
				evt[ \xy ] = XYView( parent, 16@16 );
				evt[ \y ] = SmoothNumberBox( parent, 40@16 ).value_(0);
			
				evt[ \x ].action = {	
					var values;
					values = [ evt[ \x ].value, evt[ \y ].value ];
					if( selected.size == 0 ) { this.select( \all ) };
					if( selected.size == 1 )
						{ this.setPoint( nil, values[0], values[1] ); }
						{ this.move( values[0], values[1], false, true ); }
					};
			
				evt[ \y ].action = evt[ \x ].action;	
					
				evt[\xy].mouseDownAction = { 
					evt[\hit] = (evt[\x].value)@(evt[\y].value);
					};
	
				evt[\xy].mouseMoveAction = {	
					var pt, vw;
					vw = evt[\xy];
					pt = (((vw.x)@(vw.y.neg)) * 0.1) + evt[\hit];
					evt[\x].value = pt.x;
					evt[\y].value = pt.y;
					evt[\y].doAction;
				};
			},	
					
			\scale: {	 |parent, evt| 
				
				// scale
				transformViewCreators[ \createLabel ].value( parent, evt, "scale" );
					
				evt[ \x ] = SmoothNumberBox( parent, 40@16 )
					.step_(0.1).scroll_step_(0.1).value_(1);
				evt[ \xy ] = XYView( parent, 16@16 );
				evt[ \y ] = SmoothNumberBox( parent, 40@16 )
					.step_(0.1).scroll_step_(0.1).value_(1);
				
				evt[ \x ].action = {
					var values;
					values = [ evt[ \x ].value, evt[ \y ].value ];
					if( selected.size == 0 ) { this.select( \all ) };
					this.scale( values[0], values[1], true );
					};
				
				evt[ \y ].action = evt[ \x ].action;		
				evt[\xy].mouseDownAction = { 
					evt[\hit] = (evt[\x].value)@(evt[\y].value);
					};
		
				evt[\xy].mouseMoveAction = {
					var pt, vw;
					vw =evt[\xy];
					pt = (((vw.x)@(vw.y.neg)) * 0.1) + evt[\hit];
					evt[\x].value = pt.x;
					evt[\y].value = pt.y;
					evt[\y].doAction;
				};
			},
			
			\rotate: { |parent, evt| 
				
				// rotate
				transformViewCreators[ \createLabel ].value( parent, evt, "rotate" );
			
				evt[ \amt ] = EZSmoothSlider( parent, 104@16, 
					controlSpec: [ -180, 180, \lin, 1, 0 ], numberWidth: 40 );
				
				evt[ \amt ].sliderView
					.hilightColor_( Color.gray(0.2).alpha_(0.5) )
					.centered_(true)
					.clipMode_(\wrap);
		
				evt[ \amt ].action = { |sl|
					if( selected.size == 0 ) { this.select( \all ) };
					this.rotate( sl.value, true );
					};
			},
			
			\smooth: { |parent, evt| 
				
				// rotate
				transformViewCreators[ \createLabel ].value( parent, evt, "smooth" );
			
				evt[ \amount ] = SmoothSlider( parent, 60@16 )
					.hilightColor_( Color.gray(0.2).alpha_(0.5) )
					.centered_(true)
					.value_( settings[ \smooth ][ \amount ].linlin(-1.0,1.0,0.0,1.0) )
					.action = { |sl|
						settings[ \smooth ][ \amount ] = sl.value.linlin(0.0,1.0,-1.0,1.0);
						this.doTransform( true );
						};
				
				evt[ \order ] = SmoothNumberBox( parent, 40@16 )
					.clipLo_( 3 )
					.value_( settings[ \smooth ][ \order ] )
					.action = { |nb|
						settings[ \smooth ][ \order ] = nb.value.round(1);
						this.doTransform( true );
						};
				
				/*	
				StaticText( parent, 50@16 );
				
				evt[ \type ] = PopUpMenu( parent, 60@16 )
					.canFocus_(false).font_(font)
					.items_([ 'windowed', 'mean', 'median' ])
					.action_({ |pu|
						settings[ \smooth ][ \type ] = pu.item;
						this.doTransform( true );
					});
				
				evt[ \mode ] = PopUpMenu( parent, 40@16 )
					.canFocus_(false).font_(font)
					.items_([ 'clip', 'wrap', 'fold' ])
					.action_({ |pu|
						settings[ \smooth ][ \mode ] = pu.item;
						this.doTransform( true );
					});
				*/
				
			},
		 	
			\size: { |parent, evt|
				// size
				transformViewCreators[ \createLabel ].value( parent, evt, "size" );
			
				evt[ \size ] = SmoothNumberBox( parent, 40@16 )
					.value_(path.positions.size)
					.clipLo_( 2 )
					.action_({ |nb|
						settings[ \size ][ \size ] = nb.value;
						this.doTransform( true );
						if( [ 'extend', 'prepend', 'repeat', 'fold' ]
								.includes( settings[ \size ][ \type ] ) )
							{ views[ \edit ][ \duration ][ \duration ].value =
								path.times.sum;
							};
					});
				
				evt[ \type ] = PopUpMenu( parent, 60@16 )
					.canFocus_(false).font_(font)
					.items_([ 'hermite', 'spline', 'bspline', 'linear', '-',
						'extend', 'prepend', '-', 'repeat', 'fold' ])
					.action_({ |pu| 
						var type;
						type = pu.items[ pu.value ];
						settings[ \size ][ \type ] = type;
						this.doTransform( true );
						if( [ 'extend', 'prepend', 'repeat', 'fold' ].includes( type ) )
							{ views[ \edit ][ \duration ][ \duration ].value =
								path.times.sum;
							};
						});
							
			},
			
			\duration: { |parent, evt|
				// duration
				transformViewCreators[ \createLabel ].value( parent, evt, "duration" );
			
				evt[ \duration ] = SmoothNumberBox( parent, 104@16 )
					.value_(path.times.sum)
					.clipLo_( 0 )
					.formatFunc_({ |value| value.asSMPTEString( 1000 ); })
					.action_({ |nb|
						settings[ \duration ][ \timeScale ] = 
							nb.value / (tempTimes ? path.times).sum;
						this.doTransform( true );
					});
							
			},
			
			\equal: { |parent, evt|
				// equal
				transformViewCreators[ \createLabel ].value( parent, evt, "equal" );
					
				evt[ \mode ] = PopUpMenu( parent, 50@16 )
					.canFocus_(false).font_(font)
					.items_([ 'times' , 'speeds' ])
					.action_({ |pu| 										settings[ \equal ][ \mode ] = pu.items[ pu.value ];
						this.doTransform( true );
						});
			
				evt[ \amount ] = SmoothSlider( parent, 50@16 )
					.hilightColor_( Color.gray(0.2).alpha_(0.5) )
					.action_({ |sl|
						settings[ \equal ][ \amount ] = sl.value;
						this.doTransform( true );
					});
			
			},
				
			\morph: { |positions, times|	
			}
		
		);
		
//// TRANSFORMATION VIEW SETTERS ////////////////////////////////////////
		
		transformViewSetters = (
		
			\name: { |evt|
				evt[ \name ][ \name ].string = (path.name ? "").asString;
			},
			
			\interpolation: { |evt|
				if( path.respondsTo( \curve ) ) {
					evt[ \interpolation ][ \curve ].value = path.curve;
					evt[ \interpolation ][ \hydeFunc ].value;
					{ evt[\interpolation][\type].value = evt[\interpolation][\type].items
						.indexOf( path.intType );
					  evt[\interpolation][\clip].value = evt[\interpolation][\clip].items
						.indexOf( path.intClipMode );
					}.defer
				};

			},
			
			\mode: { |evt|
				{ evt[\mode][\mode].value = evt[\mode][\mode].items
						.indexOf( settings[\mode] );
				 switch(  settings[\mode],
				 	'cartesian', { 
					 	views[ \plot ][ \x ][ \label ].string = "x";
					 	views[ \plot ][ \y ][ \label ].string = "y";
				 	},
				 	'polar', {
					 	views[ \plot ][ \x ][ \label ].string = "distance";
					 	views[ \plot ][ \y ][ \label ].string = "angle";
				 	});
						 }.defer;
			},
			\move: { |evt| // views[ \edit ]	
					if( selected.size == 1 )
						{ 
						if( settings[ \mode ] == \polar )
							{  // change later
							evt[\move][\x].value = path.positions[selected][0].x;
							evt[\move][\y].value = path.positions[selected][0].y;
							} { 
							evt[\move][\x].value = path.positions[selected][0].x;
							evt[\move][\y].value = path.positions[selected][0].y;
							};
						} {
						evt[\move][\x].value = settings[ \move ].x;
						evt[\move][\y].value = settings[ \move ].y;
						};
				},
			\scale: { |evt|
				evt[\scale][\x].value = settings[ \scale ].x;
				evt[\scale][\y].value = settings[ \scale ].y;
				},
			\rotate: { |evt|
				evt[\rotate][\amt].value = settings[ \rotate ];
				},
			\smooth: { |evt|
				evt[\smooth][\amount].value = settings[ \smooth ][ \amount ].linlin(-1.0,1.0,0.0,1.0);
				evt[\smooth][\order].value = settings[ \smooth ][ \order ];
				/*
				{ evt[\smooth][\type].value = evt[\smooth][\type].items
						.indexOf( settings[\smooth][\type] );
				  evt[\smooth][\mode].value = evt[\smooth][\mode].items
						.indexOf( settings[\smooth][\mode] );
					}.defer;
				*/
				},
			\size: { |evt|
				evt[\size][\size].value = settings[ \size ][ \size ];
				{ evt[\size][\type].value = evt[\size][\type].items
						.indexOf( settings[\size][\type] ) }.defer;
				},
			\duration: { |evt|
				evt[ \duration ][ \duration ].value = settings[ \duration ][ \timeScale ] *
						( tempTimes ? path.times ).sum;
				},
			\equal: { |evt|
				evt[\equal][\amount].value = settings[ \equal ][ \amount ];
				
				{ evt[\equal][\mode].value = evt[\equal][\mode].items
						.indexOf( settings[\equal][\mode] ) }.defer;
				},
			\morph: { |evt|
			}
		);
		
	}
	
//// TRANSFORMATION CREATE GUI ////////////////////////////////////////

	createTransformViews {
		views[ \edit ] = (); // change to views[ \transform ] later
		
		views[ \edit ][ \name ] = ();
		transformViewCreators[ \name ].value( views[ \edit_comp ], views[ \edit ][ \name ] );
		
		views[ \edit ][ \interpolation ] = ();
		transformViewCreators[ \interpolation ].value( views[ \edit_comp ], 
			views[ \edit ][ \interpolation ] );
			
		views[ \edit ][ \mode ] = ();
		transformViewCreators[ \mode ].value( views[ \edit_comp ], views[ \edit ][ \mode ] );
		
		transformOrder.do({ |key, i|
			views[ \edit ][ key ] = ();
			transformViewCreators[ key ].value( views[ \edit_comp ], views[ \edit ][ key ] );
		});
	}
	
	
//// PLOT VIEWS ////////////////////////////////////////
	
	createPlotViews {
		
		StaticText(  views[ \edit_comp ], 158@16 );
		
		views[ \plot ] = ();
		
		transformViewCreators[ \createLabel ].value( views[ \edit_comp ], 
					views[ \plot ], "plot" );
		/*			
		views[ \plot ][ \label ] = StaticText( views[ \edit_comp ], 50@16 )
			.font_( font ).align_( \right )
			.string_( "plot" );
		*/
		views[ \plot ][ \mode ] = PopUpMenu( views[ \edit_comp ], 104@16 )
					.canFocus_(false).font_(font)
					.items_([ 'absolute', 'relative', 'off' ])
					.action_({ |pu| 
						settings[ \plot ][ \mode ] = pu.items[ pu.value ];
						this.updateGUI;
						//this.doTransform( true );
						});
		
		[ \x, \y, \time ].do({ |key|
			views[ \plot ][ key ] = ();
			
			if( key != 'time' )
				{	
				transformViewCreators[ \createLabel ].value( views[ \edit_comp ], 
					views[ \plot ][ key ], key.asString );
				
				/*
				views[ \plot ][ key ][ \label ] = StaticText( views[ \edit_comp ], 50@16 )
							.font_( font ).align_( \right )
							.string_( key.asString );
				*/
				} {
				views[ \plot ][ key ][ \typeSwitch ] = PopUpMenu( views[ \edit_comp ], 50@16 )
							.font_( font )
							.items_(['time', 'speed', 'doppler'])
							.value_( ['time', 'speed', 'doppler']
									.indexOf( settings[ \plot ][ \time ] ) )
							.action_({ |pu|
								settings[ \plot ][ \time ] = pu.items[ pu.value ];
								views[ \plot ][ key ][ \view ].refresh;
							});
				views[ \plot ][ key ][ \type ] = 'time';
			};
			views[ \plot ][ key ][ \view ] =
				 UserView(  views[ \edit_comp ], 104@40 ); //.background_( Color.white );
			
			views[ \plot ][ key ][ \view ].drawFunc = { |vw|
				var rect, pos, min, max, size, left, right, zeroLine;
				
				rect = vw.drawBounds;
				
				if( settings[ \plot ][ \mode ] != \off )
				{	// active
	
					Pen.fillColor = Color.gray(0.8);
					Pen.roundedRect( rect, 5 );
					Pen.fill;
					
					rect = rect.insetBy(2,2);
					pos = switch( key, 
						\x, { 
							if( settings[ \mode ] == 'polar' )
								{ path.positions.collect(_.dist(0)).round(10e-12); }
								{ path.positions.collect(_.x); };
							},
						\y, {if( settings[ \mode ] == 'polar' )
								{ path.positions.collect(_.theta).unwrap2(pi); }
								{ path.positions.collect(_.y); };
							},
						\time, { switch( settings[ \plot ][ \time ],
							'time', { ([0] ++ path.times.round(10e-12)).integrate },
							'speed', { path.speeds.round(10e-12) },
							'doppler', {
								 path.positions.collect({ |item|
									 	 item.asPoint.asPolar.rho })
									  .differentiate[1..].round(10e-12).neg / path.times
							});
							}); 
					if( settings[ \plot ][ \mode ] === \relative )
						{ pos = pos.differentiate[1..] };
					if( pos.size > 0 )
					{
					
					max = pos.maxItem + 0.00001;
					if( key === \time && { settings[ \plot ][ \mode ] === \absolute } )
						 { min =  pos.minItem.min(-0.1); }
						 { min = pos.minItem - 0.00001; };
					size = pos.size;
					left = rect.left;
					right = rect.right;
					
					if( (zeroLine = 0.linlin( min, max, rect.bottom, rect.top ))
							.exclusivelyBetween( rect.top, rect.bottom ) )
						{ Pen.width = 0.5;
						  Pen.strokeColor = Color.gray(0.4);
						  Pen.line( 
						  		(rect.left - 2) @ zeroLine, 
						  		(rect.right + 2) @ zeroLine )
						  	.stroke;
						};
					
						
					Pen.width = 1;
					Pen.strokeColor = Color.gray(0.2);
					
					Pen.moveTo( left@(pos[0].linlin( min, max, rect.bottom, rect.top ) ) );
					pos.do({ |item,i|
						item = item.linlin( min, max, rect.bottom, rect.top);
						Pen.lineTo( i.linlin(0,size,left,right)@item );
						Pen.lineTo( (i+1).linlin(0,size,left,right)@item );
						});			
					Pen.stroke;
					}
				} { // inactive
				Pen.fillColor = Color.gray(0.6);
				Pen.roundedRect( rect, 5 );
				Pen.fill;
				};
			};
		});
		 
		
	}
	
//// GENERAL METHODS ////////////////////////////////////////
	
	fit { |includeCenter = true|
			if( includeCenter )
			{ xyView.viewRect_( path.asRect.scale(1@(-1))
					.union( Rect(0,0,0,0) ).insetBy(-1,-1) );  }
			{ xyView.viewRect_( path.asRect.scale(1@(-1)).insetBy(-1,-1) ); }
		 }
	
	path_ { |newPath| 
			this.finalize; // save current state (if changed)
			path = newPath; changed = true;   // replace path
			this.finalize( \new_path, true ); // update gui 
		}
	
//// UNDO METHODS ////////////////////////////////////////
	
	addToUndoHistory { |path, msg|
		undoHistory = [ [ path.copyNew, msg, Date.localtime ] ] ++ (undoHistory ? [])[undoStep..];
		undoStep = 0;
		if( undoHistory.size > maxUndo ) { undoHistory = undoHistory[..maxUndo+1] };
	}
	
	postUndoHistory { 
		undoHistory.reverseDo({ |item, i|
			var size;
			size = undoHistory.size;
			"%: % (%)\n".postf( size - i, item[1].asString, item[2].hourStamp[..7] );
		});
	}
	
	undo { |steps = 1|
		this.finalize;
		if( (steps + undoStep).exclusivelyBetween( -1, undoHistory.size ) )
			{ path = undoHistory[steps + undoStep][0].copyNew;
			  undoStep = steps + undoStep;
			  settings = this.defaultSettings;
			  this.updateGUI;
			} { "% :: minimum or maximum reached\n".postf( thisMethod.asString ) };
	}
	
//// SELECTION METHODS ////////////////////////////////////////

	select { |...index|
		if( index[0] === \all ) { index = path.positions.collect({ |item, i| i }) };
		selected = index.flat; 
		this.updateGUI;
	}
	
	selectNoUpdate { |...index|
		if( index[0] === \all ) { index = path.positions.collect({ |item, i| i }) };
		selected = index.flat; 
	
	}
		
//// TRANSFORMATION METHODS ////////////////////////////////////////

	setPoint { |i, x = 0, y = 0, limitDirection = false, updateGUI = true|
		changed = true;
		i = i ? selected[0] ? 0;
		path.positions[i] = (x@y);
		this.updateGUI( updateGUI );
	}

	
	move { |x = 0, y = 0, limitDirection = false, updateGUI = true|
		if( x.respondsTo( \x ) )
			{ settings[ \move ] = x.asPoint; }
			{ settings[ \move ] = x@y; };
		this.doTransform( updateGUI );
		}
		
	scale { |x, y, updateGUI = true|
		x = x ? 1;
		y = y ? x;
		if( x.respondsTo( \x ) )
			{ settings[ \scale ] = x.asPoint; }
			{ settings[ \scale ] = x@y; };
		this.doTransform( updateGUI );
		}
		
	rotate { |amt = 0, updateGUI = true|
		settings[ \rotate ] = amt;
		this.doTransform( updateGUI );
	}
	
	doTransform { |updateGUI = true|
		var defaultSettings;
		var newPos, newTimes;
		
		changed = true;
		
		tempPos = tempPos ?? { 
			if( selected.size >= path.positions.size ) { allSelected = true };
			path.positions.as( Array ); 
			};
			
		tempTimes = tempTimes ?? { path.times.copy };
		
		newPos = tempPos;
		newTimes = tempTimes;
		
		defaultSettings = this.defaultSettings; 
		
		defaultSettings[ \size ][ \size ] = tempPos.size; // force size
		
		transformOrder.reverse.do({ |key, i|
			if( settings[ key ].notNil  && { settings[ key ] != defaultSettings[ key ] }) 
			{ #newPos, newTimes = transformFuncs[ key ].value( newPos, newTimes );
			  if( allSelected && { selected.size != newPos.size } )
			  	{ selected = (..newPos.size-1); /* "boe".postln */  }
			  	{ selected = selected.select({ |item| item < newPos.size }); };
			  };
		});
		
	
		path.positions = newPos.collect(_.asWFSPoint).asWFSPointArray;
		path.forceTimes( newTimes );
		if( updateGUI ) { this.updateGUI( updateGUI ); }; 
	}
		

	updateGUI { |includeViews = true|
		xyView.refresh; timeView.refresh; 
		[\x,\y,\time].do({ |key|
				views[ \plot ][ key ][ \view ].refresh;
			});
		transformViewSetters.do({ |func| func.value( views[ \edit ] ); });
		views[ \undo ][0].enabled = undoStep > 0;
		views[ \undo ][1].enabled = (undoStep +1) <= (undoHistory.size -1);
	}
	
	finalize { |actionName = 'finalize', force = false|
		if( tempPos.notNil or: force )
		{	if( changed ) { this.addToUndoHistory( path, actionName ); changed = false; };
			allSelected = false;
			tempPos = nil;
			tempTimes = nil;
			settings = this.defaultSettings;
			this.updateGUI;
		}
	}
	
	
//// RECORDING METHODS ////////////////////////////////////////
	
	startRecord { |point, clearPath = true, addTime = 0.1|
		recordLastTime = Process.elapsedTime;
		if( clearPath )
			{ path.positions = [ point.asWFSPoint ];
			  path.forceTimes([]);
			} {
			  path.positions = path.positions ++ [ point.asWFSPoint ];
			  path.forceTimes( path.times ++ [ addTime ] );
			};
	}
	
	recordPoint { |point| // adds point to end
		var newTime, delta;
		if( recordLastTime.notNil ) // didn't start recording yet
			{
			newTime = Process.elapsedTime;
			path.forceTimes( path.times ++ [ newTime - recordLastTime ] );
			path.positions = path.positions ++ [ point.asWFSPoint ];
			recordLastTime = newTime;
			} 
			{ "%: didn't start recording yet\n".postf( thisMethod ); };	}
			
	endRecord {
		recordLastTime = nil;
	}
	
}

	/*
	transform { |move, scale, rotate, updateGUI = true |
		
		///// OLD 
		
		changed = true;
		tempPos = tempPos ?? { path.positions.collect( _.asPoint ) };
		
		move = (move ? settings[\move]).asPoint;
		scale = (scale ? settings[\scale]).asPoint;
		rotate = (rotate ? settings[\rotate]).asFloat.wrap(-180,180);
		
		settings[ \move ] = move;
		settings[ \scale ] = scale;
		settings[ \rotate ] = rotate;
		
		path.positions = path.positions.collect({ |pt, i|
			if( selected.includes( i ) )
				{   (tempPos[i].asPoint
				   		.rotate( rotate.linlin(-180,180,-pi,pi,\none) ) 
				   	* scale + move).asWFSPoint; }
				{ pt };		
			});
		
		this.updateGUI( updateGUI );
	}
	*/