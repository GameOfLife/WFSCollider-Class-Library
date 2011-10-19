/*
    GameOfLife WFSCollider - Wave Field Synthesis spatialization for SuperCollider.
    The Game Of Life Foundation. http://gameoflife.nl
    Copyright 2004-2010 Wouter Snoei.

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

// to do : optimizing and cleaning
//

// there is never more then one path editor open
// path data is stored in classvars and kept even after the path editor
// window is closed. The window can be reopened with .newAgain
// .new will allways replace existing data
// .add / .addAll will also replace existing data when window is closed


WFSPathEditor_OLD {

	classvar <window = nil;
	classvar <emptyWindow = nil;
	classvar <lvRealMulti, <lvNames, <lvTimes;
	classvar <lvReal; //current path
	classvar <times, <currentIndex = 0;
	classvar <selected;
	classvar <refreshViews = nil;
	classvar <addWFSPath = nil;
	classvar <>plotSmoothWindow;

	*paths {
		^WFSPathArray.fill( lvRealMulti.size, { |i|
			WFSPath(lvRealMulti[i].flop, name: lvNames[i].asSymbol).timeLine_( lvTimes[i] ) });
			}	
	
	*at { |index = 0|
		if( index.isSymbol or: index.isString )
			{ index = lvNames.indexOf( index.asSymbol ) };
		^this.paths[ index ];
		}	
	
	*current {
		if( window.notNil )
			{ ^WFSPath( lvReal.flop, name: lvNames[selected.value].asSymbol ).timeLine_(times); }
			{ ^nil }; 
		}
	
	*close { if( window.notNil ) { window.close; } }
	
	*add { |aWFSPath|
		aWFSPath = aWFSPath.asWFSPath;
		if( window.isNil or: { window.isClosed })
			{ WFSPathEditor([aWFSPath]) }
			{ addWFSPath.value( aWFSPath ); };
		
		}
	
	*addAll { |array|
		array = array.asWFSPathArray;
		if( window.isNil or: { window.isClosed })
			{ WFSPathEditor(array) }
			{ array.do({ |item|
				addWFSPath.value( item ); })
			};
		}
		
	*new { arg wfsPaths, windowName = "WFSPathEditor";
		if(wfsPaths.notNil)
			{ WFSPathEditor.newEditor( wfsPaths, windowName); }
			{ if( window.notNil && { window.dataptr.notNil } )
				{ window.front; }
				{ WFSPathEditor.newEmptyWindow; }
			 };
		}
		
	*newEmptyWindow {
		var ewb;
		
		if( window.notNil ) {  
			window.onClose.value; 
			window.close; window = nil;
			};
			
		if( emptyWindow.isNil )
			{ 	ewb = Rect(64 + 64.rand, 32 + 32.rand , 650, 700);
				emptyWindow = SCWindow.new("WFSPathEditor", ewb, false);
				emptyWindow.front;
				StaticText( emptyWindow, Rect.aboutPoint( 
						(ewb.width / 2)@(ewb.height * 0.125), 210, 40 ) 
						)
					.string_( "The WFSPathEditor is empty" )
					.font_( Font("Helvetica-Bold", 30) )
					.stringColor_( Color.black.alpha_(0.5) );
				
				RoundButton( emptyWindow, Rect.aboutPoint( 
						(ewb.width / 2)@((ewb.height * 0.25) + 25 ) , 80, 10 ) )
					.radius_(0).border_(1).states_( [ ["generate new WFSPath" ] ] )
					.action_({ WFSPathEditor.newEditor( 
						[ WFSPath( [ [0,0],[1,0] ], name: \temp ) ], "WFSPathEditor", true ); });
						
				RoundButton( emptyWindow, Rect.aboutPoint( 
						(ewb.width / 2)@((ewb.height * 0.25) + 50 ) , 80, 10 ) )
					.radius_(0).border_(1).states_( [ ["draw new WFSPath" ] ] )
					.action_({  WFSDrawWindow.new; });
				
				RoundButton( emptyWindow, Rect.aboutPoint( 
						(ewb.width / 2)@((ewb.height * 0.25) + 75 ) , 80, 10 ) )
					.radius_(0).border_(1).states_( [ ["open file" ] ] )
					.action_({   
						CocoaDialog.getPaths(
							{arg paths;
								//WFSPathEditor.close;
								WFSPathEditor( WFSPathArray.readWFSFile(paths.first) );
								}, { "cancelled".postln; });
						});
						
				RoundButton( emptyWindow, Rect.aboutPoint( 
						(ewb.width / 2)@((ewb.height * 0.25) + 100 ) , 80, 10 ) )
					.radius_(0).border_(1).states_( [ ["import SVG file" ] ] )
					.action_({   
						CocoaDialog.getPaths(
							{arg paths;
								var file;
								file = SVGFile.read( paths[0] );
								if( file.hasCurves )
									{ SCAlert( "file '%'\nincludes curved segments. How to import?"
										.format( file.path.basename ),
										[ "cancel", "lines only", "curves" ],
										[ {},{  WFSPathEditor( WFSPathArray
												.fromSVGFile( file, useCurves: false) ) },
										  {  WFSPathEditor( WFSPathArray
										  		.fromSVGFile( file, useCurves: true) ) } ]  
										); }
									{ WFSPathEditor( WFSPathArray.fromSVGFile( file ) ) }
								//WFSPathEditor.close;
								}, { "cancelled".postln; });
						});
						
						
				SCPopUpMenu( emptyWindow, Rect.aboutPoint( 
						(ewb.width / 2)@((ewb.height * 0.25) + 125 ) , 80, 10 ) )
					.items_( [ "(create default WFSPath", /*)*/ "-", 
						"circle", "rand", "spiral", "lissajous", "line", "rect" ] )
					.action_({ |popup|
						if( popup.value != 0 )
							{ "WFSPath.%.edit;".format( popup.items[ popup.value ] ).interpret; }
						})
					.background_( Color.gray(0.7) );
						
				emptyWindow.onClose_({ emptyWindow = nil });
				}
			{ emptyWindow.front };
				
		^emptyWindow
		}
	
	*newAgain { arg windowName = "WFSPathEditor";
		WFSPathEditor( WFSPathEditor.paths, windowName )
		}
			
	*newEditor { arg wfsPaths, windowName = "WFSPathEditor", generateNewAtStartup = false; 
		// private, does not create an instance!!
		
		
		// alphabetized vars
		
		var addMovement, addPoint, animate, animateFunc;
		var centerBoxes, centerMode, centerV, clipboardPoint, copyMovement;
		var deZoom, durationView;
		var equalTime;
		var fillMorphTo, filter, fit, fitAll, fitView, fix, fixTime, fixView, flipButtons, flopFunc;
		var generateWindow, gw;
		var history, historySwitch;
		var instance, intCloseOpen, intCurve, intCurveControls, intType, interpolateBox;
		var lastSpeeds, lb, lbSize, localIndexes, ls, lsMulti, lw, lxy, lxyReal;
		var makeDisplay, makeMultiDisplay, makePreset, morph, morphTo, move, multipleText;
		var nAngle, nBoxes, nBoxesStart, nDist, nPoint, nRel, nSpeed, nTime, nX, nY, numEdit;
		var originalMulti, originalTimes, originalValue;
		var postPath, presetSwitch, presets;
		var quantize, quantizeTime;
		var removeMovement, renameMovement, removeMultiDisplay, repeat, resetPreset, resetView, restAlpha, reverse;
		var rotate, rotateBox, rotateStep;
		var saveAll, savePath, scale2D, scale2Dboxes, scaleTime, scl, selectMovement, selectView, speedScale;
		var testSynth;
		var timeBack, timeBackFill, timeFill, timeGrid, timeScale, timeSelectView;
		var timeString, timeView, transform, transformTime;
		var usePreset;
		var visualMode;
		var warpTime;
		var zoom, zoomView;
		
		var autoPlotSmooth = false;
		
		// assigned vars
		
		var centerOffset = 450;
		var tvOffset = 510;
		var lockSpeeds = false;
		var colorAgain = true;
		var alphaCheck = 0.0;
		
		if( emptyWindow.notNil ) {  { emptyWindow.close; }.try; emptyWindow = nil };
		if(wfsPaths.isArray.not) { wfsPaths = [wfsPaths] };
		if(wfsPaths.any( _.isWFSPath.not )) { "invalid input arguments for WFSPathEditor".warn };
		
		lvRealMulti = wfsPaths.collect({ |wfsPath| wfsPath.asXYArray.flop });
		lvTimes = wfsPaths.collect( _.timeLine );
		times = lvTimes.at(0).copy;
		lvNames = wfsPaths.collect({ |item| item.name });
			
		
		if(window.notNil) { 
			window.onClose.value; 
			window.close;};
		
		lw = SCWindow(windowName, Rect(64 + 64.rand, 32 + 32.rand , 650, 700), false).front;
		
		
		//window = lw;
		
		lw.onClose = {
			if(gw.notNil) {gw.close}; 
			};
		
		/* StaticText.new(lw, Rect(52,13,250,18)).string_("positions / spatial path (x,y)")
			.stringColor_(Color.black.alpha_(0.75)); 
		*/
		
		resetPreset = ('name': 'reset',
					'scale': [1,1],
					'rotate': 0,
					'flipX': 0,
					'flipY': 0,
					'reverse': 0,
					'repeat': 0,
					'move': [0,0],
					'quantize': 0,
					'interpolate': 1,
					'close': 1,
					'type': nil,  //not implemented
					'splineAutoCurve' : nil, //not implemented
					'curve': nil, //nil means no change
					'morph': 0);
					
		presets = [resetPreset];
		
		makePreset = {arg name;
				('name': name,
				'scale': [scale2Dboxes.at(0).value, scale2Dboxes.at(1).value],
				'rotate': rotateBox.value,
			
				'flipX': flipButtons.at(0).value,
				'flipY': flipButtons.at(1).value,
			
				'reverse': reverse.value,
				
				'repeat': repeat.value,
				'move': [move.at(0).value, move.at(1).value],
				'quantize': quantize.value,
				'interpolate': interpolateBox.value,
			
				'close': intCloseOpen.value,
				
				'type': nil,
				'splineAutoCurve': (intCurveControls.at(1).value == 1),
				'curve': intCurveControls.at(0).value,
				'morph': morph.value); };
		
		usePreset = {arg preset; 
			if(preset.isNil) {preset = resetPreset};
			move.at(0).value = preset.at('move').at(0); 
			move.at(1).value = preset.at('move').at(1); 
			rotate.value = preset.at('rotate') / 360;
			rotateBox.value = preset.at('rotate');
			repeat.value = preset.at('repeat');
			quantize.value = preset.at('quantize');
			interpolateBox.value = preset.at('interpolate');
			
			intCloseOpen.value = preset.at('close');
			flipButtons.at(0).value = preset.at('flipX');
			flipButtons.at(1).value = preset.at('flipY');
			reverse.value = preset.at('reverse');
			
			morph.value = preset.at('morph');
			scale2D.setXY( *(preset.at('scale') / 2) );
			scale2Dboxes.at(0).value = preset.at('scale').at(0);
			scale2Dboxes.at(1).value = preset.at('scale').at(1);
			};
					
		lvReal = lvRealMulti.at(0);
		
		nBoxesStart = [0, 1]; //initial zoom point [x min, x max, y min, y max] -- wrapped
		intType = ['spline'];
		
		/* StaticText.new(lw, Rect(0, 50, 48, 20))
			.string_("paths").stringColor_(Color.black.alpha_(0.75)).setProperty( \align, \right );
		*/
		
			{ var lastItem = 0;
			[102,  // scale
			42,  // rotate
			44, // flip/repeat
			42, // move
			22, // quantize
			62, // interpolate
			22 // morph
			].do({ |item, i| 
				SCCompositeView(lw,Rect(498, lastItem, 152 ,item)).background_( Color.hsv(((i+1) / 4) % 1.0, 1.0, 
					[0.8, 0.4].wrapAt(i)).alpha_(0.125) ); 
				lastItem = lastItem + item;
				});
		}.value;
		
		SCCompositeView(lw,Rect(496,0 , 154 , 372)).background_( Color.white.alpha_(0.25) );  // presets
		SCCompositeView(lw,Rect(476, 378 ,174, 58)).background_( Color.black.alpha_(0.125) );  // keep/history
		SCCompositeView(lw,Rect(496, 446 , 154 , 45)).background_( Color.white.alpha_(0.25) );  // centermode
		
		
		presetSwitch = SCPopUpMenu.new(lw, Rect(500, 350, 80, 18));
		presetSwitch.items_(presets.collect({arg item; item.at('name'); }) ++ ['-', 'new...']);
		StaticText.new(lw, Rect(582, 350, 80, 18)).string_("presets");
		presetSwitch.addUniqueMethod('select', {arg popUp, mode = true;
						if(mode)
							{presetSwitch.background_(Color.black);
							presetSwitch.stringColor_(Color.gray(0.8));}
							{presetSwitch.background_(Color.clear);
							presetSwitch.stringColor_(Color.black);}
						});
		presetSwitch.action = {arg popUp;
				var prName;
				var newPreset;
				prName = presetSwitch.items.at(popUp.value);
				if(prName == 'new...')
					{
								newPreset = makePreset.value( ('pr' ++ (presets.size + 1).asSymbol).asSymbol );
					presets = presets.add(newPreset);
					presetSwitch.items_(presets.collect({arg item; item.at('name'); }) ++ ['-','new...', 'clear all...']);
					presetSwitch.value = presets.size - 1;
					presetSwitch.select(true); }
					{
					if(prName == 'clear all...')
						{presets = [resetPreset];
						presetSwitch.items_(presets.collect({arg item; item.at('name'); }) ++ ['-', 'new...']);
						presetSwitch.value = 0;
						presetSwitch.select(false);
						}
					{usePreset.value(presets.at(presetSwitch.value));
					transform.value(true);}
						 };
				};
				
		
		history = [[lvRealMulti.copy, lvNames.copy, lvTimes.copy]];
		
				
		historySwitch = SCPopUpMenu.new(lw, Rect(480, 414, 80, 18));
		historySwitch.addUniqueMethod('itemFill', {
			historySwitch.items_(
				history.collect({arg item, i;
					( "state " ++ (i+1).asString).asSymbol  }) ++ ['-', 'clear all...']);
			});
		historySwitch.itemFill;
		StaticText.new(lw, Rect(562, 414, 88, 18)).string_("undo history");
		historySwitch.addUniqueMethod('select', {arg popUp, mode = true;
						if(mode)
							{historySwitch.background_(Color.black);
							historySwitch.stringColor_(Color.gray(0.8));}
							{historySwitch.background_(Color.clear);
							historySwitch.stringColor_(Color.black);}
						});
						
		historySwitch.addUniqueMethod('addItem', {
					history = history.add([lvRealMulti.copy, lvNames.copy, lvTimes.copy]);
					historySwitch.itemFill;
					historySwitch.value = history.size -1;
					});
					
		historySwitch.action = {
						if (historySwitch.value < history.size)
							{lvRealMulti = history.at(historySwitch.value).at(0).copy;
							lvNames = history.at(historySwitch.value).at(1).copy;
							lvTimes = history.at(historySwitch.value).at(2).copy;
							times = lvTimes.at(selected.value);
							originalValue = lvRealMulti.at(selected.value);
							originalTimes = nil;
							selected.items_(lvNames.copy ++ ['-','all']);
							fillMorphTo.value;
							selected.valueAction_(0);
							colorAgain = true;
							transform.value;
							fitAll.value;
							}
							{history = [[lvRealMulti.copy, lvNames.copy, lvTimes.copy]];
							historySwitch.itemFill;
							historySwitch.value = 0;}
							};
		
		
		RoundButton.new(lw, Rect(476, 350, 18, 18)).radius_(0).border_(1).states_([["->", Color.black, Color.white.alpha_(0.25)]]).action_({
					usePreset.value(presets.at(presetSwitch.value));
					transform.value(true); });
		
		
		scl = {arg in, inMin, inMax, outMin = 0, outMax = 1; //standard scaling function
				(((in - inMin) / (inMax - inMin)) * (outMax - outMin)) + outMin;
				};
				
		zoom = { //zoom and display paths
			var newValue, oldN, fillColorArray, lastIndex;
			var plotSmoothPath;
			
			if(currentIndex >= lvReal.at(0).size)
					{currentIndex = lvReal.at(0).size - 1};
			if (lxyReal.isNil) {lxyReal = scl.value(lxy.value, 0,1,-1,1)};
			newValue = nBoxes.collect({ |item| item.value});
			
			oldN = ls.size;
			ls.value = scl.value(lvReal, newValue.at([0,2]), newValue.at([1,3]), 0,1);
			(lsMulti.copyRange(0, (lsMulti.size - 2))).do({arg item, i;
				item.value =  scl.value(lvRealMulti.at(localIndexes.at(i)), newValue.at([0,2]), newValue.at([1,3]), 0,1);
				});
		
			if(ls.index != -1)
				{currentIndex = ls.index;};
			
			timeFill.value;
			
			if 	((lvReal.at(0).size != oldN) or: (colorAgain))
				
				{
				fillColorArray = Array.fill(ls.size, {|i|
					 Color.new(
							(((1 - (i/ls.size)) * 2) - 0.8).max(0).min(1),
							((i/ls.size) * 2).fold2(1), 
							(((i/(ls.size)) * 2 ) - 0.8).max(0).min(1) )
					});
							
				ls.size.do({arg i;
					ls.setFillColor(i, fillColorArray.at(i));
					
					timeView.setFillColor(i, fillColorArray.at(i));
							
					}); 
			
			};
			
			colorAgain = false;
			lxy.value = scl.value(lxyReal, newValue.at([0,2]), newValue.at([1,3]), 0,1);
			
			selectView.value_(ls.value.copy.flop.at(currentIndex).collect({ |item| [item] }));
			
			// plotSmoothAlways
			if( autoPlotSmooth )
				{   plotSmoothPath = this.current;
					plotSmoothPath.currentTime = 0;
					plotSmoothPath.plotSmooth( toFront: false );
				};
			
				
			//timeString.updatePos;
		
			};
			
		deZoom = { //convert actual display to path with zoom correction
			var newValue;
			ls = lsMulti.last;
			newValue = nBoxes.collect({ |item| item.value});
			lvReal = scl.value(ls.value, [0,0],[1,1], newValue.at([0,2]), newValue.at([1,3]));
			zoom.value;
			};
			
		StaticText( lw, Rect(0, 32 + 45, 48, 18 ) ).string_( "zoom" ).setProperty( \align, \center );
			
		zoomView = Array.fill(2, { |i|  // +/- zoom buttons
			var local;
			local = RoundButton.new(lw, Rect([5,27].at(i), 50 + 45 ,18,18));
			local.states = [[['-', '+'].at(i)]];
			local.radius_(0).border_(1);
			local.action = {
				nBoxes.do({|item, ii| 
					item.value = [item.value + [-0.5, 0.5].wrapAt(ii), (item.value * 0.5).round(0.1)].at(i);
					});
				zoom.value;
				};	
			});
			
		resetView = RoundButton.new(lw, Rect(5, 70 + 45, 40, 18)).radius_(0).border_(1).states_([["reset"]]); //reset zoom to initial
		resetView.action = {nBoxes.do({|item, i| item.value = nBoxesStart.wrapAt(i)});
				zoom.value};
				
		fit = {  //fit zoom amount to path
			var max, min, out;
			min = [lvReal[0].minItem, lvReal[1].minItem];
			max = [lvReal[0].maxItem, lvReal[1].maxItem];
			out = [min.minItem, max.maxItem].round(0.1) + [-0.1,0.1];
			nBoxes.do({|item,i| item.value = out.wrapAt(i)});
			zoom.value;	
			};
		fitView = RoundButton.new(lw, Rect(5, 90 + 45, 40, 18)).radius_(0).border_(1).states_([["fit"]]); //(115, 10, 40, 20)
		fitView.action = {fit.value};
		
		fitAll = {
			var max, min, out;
			var minmaxX, minmaxY;
			minmaxX = (lvRealMulti.copy.add(lvReal)).collect({arg item, i; item.at(0); });
			minmaxY = (lvRealMulti.copy.add(lvReal)).collect({arg item, i; item.at(1); });
			min = [minmaxX.flat.minItem, minmaxY.flat.minItem];
			max = [minmaxX.flat.maxItem, minmaxY.flat.maxItem];
			out = [min.minItem, max.maxItem].round(0.1) + [-0.1,0.1];
			nBoxes.do({|item,i| item.value = out.wrapAt(i)});
			zoom.value;	
					};
					
		RoundButton.new(lw, Rect(5, 110 + 45, 40, 18)).radius_(0).border_(1).states_([["fit all"]]).action_({fitAll.value});
		
		/*
		visualMode = SCPopUpMenu.new(lw, Rect(100, 452, 140, 18)) // (350, 472, 100, 18)
				.items_(["show lines and points", "show points only", 
						"show lines only", "hide lines and points"])
				.action_({selectMovement.value});
		*/
				
		StaticText( lw, Rect( 5, 187, 40, 18 ) ).string_( "show" )
			.align_( \center );
			
		visualMode = [
			RoundButton( lw, Rect( 5, 205, 40, 18 ))
				.radius_(0).border_(1).states_( [ [ "lines", Color.black, Color.clear ], 
						   [ "lines", Color.gray(0.7), Color.black ] ] )
				.value_(1)
				.action_({ selectMovement.value }),
			RoundButton( lw, Rect( 5, 225, 40, 18 ))
				.radius_(0).border_(1).states_( [ [ "points", Color.black, Color.clear ], 
						   [ "points", Color.gray(0.7), Color.black ] ] )
				.value_(1)
				.action_({ selectMovement.value });
			];

		
			
		
		
		/*		
		SCPopUpMenu.new(lw, Rect(250, 452, 100, 18)) // (350, 472, 100, 18)
				.items_(["(plot smooth..", "-", "current", "all"])
				.action_({ |popUp|
				
				if(popUp.value == 2)
					{ this.current.plotSmooth };
				if(popUp.value == 3)
					{ this.paths.plotSmooth };
					
				popUp.value = 0;
				
				});
		*/
		
		RoundButton( lw, Rect( 114, 0, 40, 18 ) )
			.radius_(0).border_(1).states_( [ ["plot", Color.black, Color.clear ] ] )
			.action_({  this.current.plotSmooth  });
			
		RoundButton( lw, Rect( 198, 0, 60, 18 ) )
			.radius_(0).border_(1).states_( [ ["plot all", Color.black, Color.clear ] ] )
			.action_({  this.paths.plotSmooth  });
			
		RoundButton( lw, Rect( 156, 0, 40, 18 ) )
			.radius_(0).border_(1).states_( [ [ "auto", Color.black, Color.clear ], 
				[ "auto", Color.gray(0.7), Color.black ] ] )
			.action_( { |button| 
				var plotSmoothPath;
				autoPlotSmooth = ( button.value == 1 );
				if( button.value == 0 )
					{ WFSPlotSmooth.close; } 
					{    plotSmoothPath = this.current;
						plotSmoothPath.currentTime = 0;
						plotSmoothPath.plotSmooth( toFront: true );
				 	};
				} ); 
				
		
				
		//StaticText.new(lw, Rect(270, 452, 78, 18)).string_("show").setProperty(\align,\right);
				
		
		centerV = {arg array;   //calculate center value
					[array[0].sum / array[0].size,
					array[1].sum / array[1].size]
					};
		
		lxy = SCEnvelopeView(lw, Rect(50, 50, 400, 400))  //xy plane display 
				.drawLines_(true)
				.resize_( 5 )
				.value_([[0.0, 1.0, 0.5, 0.5], [0.5, 0.5, 0.0, 1.0]]);
				//.editable_(false);
				lxy.connect(0, [1.0]);
				lxy.connect(2, [3.0]);
				/* summer 2004 version
			lxy.select(0); lxy.connect([1.0]);
			lxy.select(2); lxy.connect([3.0]);
			*/
		lxy.drawLines_(true); lxy.drawRects_(false);
		lxy.setProperty(\strokeColor, Color(0.0, 0.0, 0.0, 0.5));
		lxy.setProperty(\thumbWidth, [-1, 1.0]); lxy.setProperty(\thumbHeight, [-1, 1.0]); 
		
		selectView = SCEnvelopeView(lw, Rect(50, 50, 400, 400)) 
				.resize_( 5 )
				.drawLines_(false)
				.drawRects_(true);
		selectView.setProperty(\thumbWidth, [-1, 5.0]); selectView.setProperty(\thumbHeight, [-1, 5.0]); 
		selectView.setProperty(\fillColor, [-1, Color.yellow.alpha_(0.8)]);
		selectView.setProperty(\strokeColor, Color.black);
		selectView.value_([0.5,0.5].collect({ |item| [item] }););
		//selectView.select(0); 
		
		
		
		makeDisplay = {arg value, alpha = 0.5, name = "", index;
			var out;
			out = SCEnvelopeView(lw, Rect(50, 50, 400, 400));  //path display (on top of xy plane display)
			out.action = {
					fixView.makeRed;
					deZoom.value;
					};
			/*
			[{out.drawLines_(true); out.drawRects_(true) },
			{out.drawLines_(false); out.drawRects_(true) },
			{out.drawLines_(true); out.drawRects_(false) },
			{out.drawLines_(false); out.drawRects_(false)}].at(visualMode.value).value;
			*/
			
			out.drawLines_( visualMode[0].value == 1 ); 
			out.drawRects_( visualMode[1].value == 1 );
			
			out.strokeColor_(Color.black.alpha_(alpha));
			out.fillColor_(Color.black.alpha_(alpha));
			out.value_(value);
			out.addUniqueMethod('name', {name});
			out.setProperty(\thumbWidth, [-1, 5.0]);
			out.setProperty(\thumbHeight, [-1, 5.0]);
			out.resize_( 5 );
			out;
			};
		
		
		makeMultiDisplay = {arg values, names, select = 0;
			var localValues, size, out;
			if(select.class == Symbol)
				{if (select !== 'all')
					{select = names.indexOf(select); }; };	
			localValues = values.copy;
			size = localValues.size-1;
			localIndexes = (0,1..size);
			if (select !== 'all') 
				{localValues.swap(select, size);
				localIndexes.swap(select, size);};
			
			out = localValues.collect({ |item, i|
				makeDisplay.(item,
					if (select !== 'all')			
						{ if (i < size) 
							{restAlpha.value} {1.0};}
						{if (i < size) 
							{0.6} {0.6};},
					names.at(i), localIndexes.at(i));
					});
			ls = out.last;
			ls.keyDownAction = {  arg view,char,modifiers,unicode,keycode;
				if ((unicode == 16rF700) or: (unicode == 16rF703), { 
					currentIndex = ((currentIndex + 1).wrap(0, lvReal.at(0).size - 1));
					});
				if ((unicode == 16rF701) or: (unicode == 16rF702), { 
					currentIndex = ((currentIndex - 1).wrap(0, lvReal.at(0).size - 1));
					});
				view.action.value;
				};
			out;
				};
		
		removeMultiDisplay = {arg multiDisplay;
			multiDisplay.do({ |item| item.remove });
			lw.refresh;
			};
		 
		
		nBoxes = Array.fill(4, {arg i; //number boxes for zoom
							var local;
							local = RoundNumberBox.new(lw,
									 /* // old location
									 [ Rect(8 , 241, 40, 18),
									Rect(452, 241, 40, 18),
									Rect(230, 452 , 40, 18),
									Rect(230, 30, 40, 18)].at(i)
									*/
									
									[ Rect(50 , 450, 40, 18), // x-min
									Rect(410, 450, 40, 18), // x-max
									Rect(10, 432 , 40, 18), // y-min
									Rect(10, 50, 40, 18)].at(i) // y-max
									)
										.background_(Color.clear)
										.enabled_( false );
							local.value_(nBoxesStart.wrapAt(i));
							local.step = 0.1;
							local.action = {|nBox|
								nBox.value = nBox.value.round(0.1);
								if ([nBox.value > -0.1, nBox.value < 0.1].wrapAt(i)) 
									{nBox.value = [-0.1, 0.1].wrapAt(i) };
								//nBoxesStart = nBoxes.collect({|item| item.value});
								zoom.value};
							local}
							);
							
		scale2D = SC2DSlider.new(lw, Rect(500, 0, 80, 80)); //2 dimensional scaling slider
		scale2D.addUniqueMethod('setXYw', {arg slider, xy;   //// Changed to setXYw since addition of setXY method to SC3
							if(xy.isNil) {xy = [0.5, 0.5];};
							scale2D.setProperty(\x, xy.at(0));
							scale2D.setProperty(\y, xy.at(1));
							});
							
		scale2Dboxes = Array.fill(2, { |i|
						var local;
						local = RoundNumberBox.new(lw, Rect([500, 541].at(i), 82, 39,18))
							.value_(1).steps_(0.1);
						local.action = { |nBox| 
							nBox.value = nBox.value.round(0.1);
							[{scale2D.x = nBox.value / 2}, {scale2D.y = nBox.value / 2}].at(i).value;
							transform.value;
							};
						local;
						});
						
		StaticText.new(lw, Rect(582, 82, 80, 18)).string_("scale (x,y)");
		scale2D.x = 0.5;
		scale2D.y = 0.5;
		scale2D.action = {
				if(scale2D.x != (scale2Dboxes.at(0).value / 2)) {scale2Dboxes.at(0).value = scale2D.x * 2};
				if(scale2D.y != (scale2Dboxes.at(1).value / 2)) {scale2Dboxes.at(1).value = scale2D.y * 2};
				transform.value};
				
		rotate = SmoothSlider.new(lw, Rect(500, 104, 80, 18)).value_(0).step_(1/360);
		rotate.action = {|slider| rotateBox.value = slider.value * 360;
						transform.value; };
				
		rotateBox = RoundNumberBox.new(lw, Rect(541, 124, 39, 18)).value_(0).steps_(1);
		rotateBox.action = {|nBox| 
				nBox.value = nBox.value.round(1);
				if(nBox.value < 0) {nBox.value = 359};
				if(nBox.value > 359) {nBox.value = 0};
				rotate.value = nBox.value / 360.0;
				transform.value;
				};
		StaticText.new(lw, Rect(582, 124, 80, 18)).string_("rotate");
		
		flipButtons = Array.fill(2, { |i|   //flip x/y
					var local, name;
					name = ["x", "y"].at(i);
					local = RoundButton.new(lw, Rect([500, 541].at(i), 146, 39, 18));
					local.states = [[name, Color.black, Color.clear], [name, Color.gray(0.8), Color.black]] ;		
					local.action = {transform.value};
					local.radius_(0).border_(1);
					local;
					});
		StaticText.new(lw, Rect(582, 146, 80, 18)).string_("flip");
		
		
		repeat = RoundNumberBox.new(lw, Rect(546, 168, 34, 18)).value_(0).steps_(1);
		repeat.action = { |nBox| if (nBox.value < 0) {nBox.value = 0};
						transform.value;};
		reverse = RoundButton.new(lw, Rect(500, 168, 45, 18));
		reverse.states = [	["reverse", Color.black, Color.clear], 
						["reverse", Color.gray(0.8), Color.black]];
		reverse.action = {transform.value};
		reverse.radius_(0).border_(1);
		
		StaticText.new(lw, Rect(582, 168, 80, 18)).string_("repeat");
		
		RoundButton.new(lw, Rect(500, 190, 80, 18)).radius_(0).border_(1).states_([["to center", Color.black, Color.clear]]).action_({
				var local;
				transform.value;
				local = [0,0] - centerV.(originalValue);
				move.at(0).value = local.at(0);
				move.at(1).value = local.at(1);
				transform.value });
					
		move = Array.fill(2, { |i|   //move x/y boxes
					var local;			
					local = RoundNumberBox.new(lw, Rect([500, 541].at(i), 210, 39, 18)).steps_(0.1);
					local.value = 0;
					local.action = {|nBox| nBox.value = nBox.value.round(0.1); transform.value};
					local;
					});
		StaticText.new(lw, Rect(582, 210, 80, 18)).string_("move (x,y)");
		
		
		
		quantize = RoundNumberBox.new(lw, Rect(541, 232, 39, 18)).value_(0).steps_(0.1);
		quantize.action = { |nBox| nBox.value = nBox.value.round(0.1).max(0);
						transform.value;};
		StaticText.new(lw, Rect(582, 232, 80, 18)).string_("quantize");
		
		
		interpolateBox = RoundNumberBox.new(lw, Rect(546, 254, 34, 18)).value_(1).steps_(0.1);
		interpolateBox.action = { |nBox| nBox.value = nBox.value.round(0.1).max(0.1);
						transform.value;};
		intCloseOpen = SCPopUpMenu.new(lw,Rect(500,254,45,18));
		intCloseOpen.items = ["open", "close"];
		intCloseOpen.value = 1;
		intCloseOpen.action = {	|popUp|transform.value};
				
		StaticText.new(lw, Rect(582, 254, 80, 18)).string_("interpolate");
		
		SCPopUpMenu.new(lw,Rect(500,274,80,18))
				.items_(["cubic spline", "hermite", "b-spline", "quadratic", "linear", "sinusoid x", 
					"sinusoid y", "step x", "step y"])
				.action_({ |butt| 
					intType = [  // x, y ,time
						['spline', 'spline', 'linear'], 
						['hermite'],
						['bspline', 'bspline', 'linear'],
						//['hermite_p', 'hermite_p', 'hermite'],
						['quad'], 
						['linear','linear','linear'], 
						['sine', 'linear', 'linear'],
						['linear', 'sine', 'linear'],
						['step', 'linear', 'linear'],
						['linear', 'step', 'linear'] ].at(butt.value);
					if(butt.value == 0)
				{ 	intCurveControls.at(1).visible_(true);
					intCurveControls.at(2).visible_(true);
					intCurveControls.at(1).valueAction = 0;}
				{	intCurveControls.at(0).visible_(false);
					intCurveControls.at(1).visible_(false);
					intCurveControls.at(2).visible_(false);};
					transform.value; });
		StaticText.new(lw, Rect(582, 274, 80, 18)).string_("int. type");
		
		intCurveControls = [
				RoundNumberBox.new(lw, Rect(500, 294, 39, 18))
					.value_((3/4) / ((1.9)**0.5)).steps_(0.01)
					.action_({ |nBox| intCurve = nBox.value; transform.value })
					.visible_(false),
				RoundButton.new(lw, Rect(541, 294, 39, 18))
					.radius_(0).border_(1).states_([["edit", Color.black, Color.clear],["auto"]])
					.action_({ |butt| 
						if (butt.value == 1)
							{intCurveControls.at(0).visible_(true);
							intCurve = intCurveControls.at(0).value;
							transform.value}
							{intCurveControls.at(0).visible_(false);
							intCurve = nil; transform.value}
						}),
			StaticText.new(lw, Rect(582, 294, 80, 18)).string_("int. curve")];
			
		morph = RoundNumberBox.new(lw, Rect(500, 316, 25, 18)).value_(0.0).steps_(0.05);
		morph.action = { |nBox| nBox.value = nBox.value.clip(-1, 2).round(0.00000001);
						transform.value };
						
		morphTo = SCPopUpMenu.new(lw, Rect(527, 316, 53, 18));
		morphTo.action = {transform.value};
		fillMorphTo = {morphTo.items_(['circle', '-'] ++ lvNames.copy)};
		fillMorphTo.value;
		morphTo.value = 0;
		morphTo.addUniqueMethod('getM', {
							var toM, rate;
							if(morphTo.value == 0)
								{nil}
								{
								/* 
								toM = lvRealMulti.at(morphTo.value - 2);
								 if(toM.at(0).size != lvReal.at(0).size) 
									{
									rate = (lvReal.at(0).size - 1) / (toM.at(0).size - 1);
									[	toM.at(0).interpolate( rate, 'linear', false),
										toM.at(1).interpolate( rate, 'linear', false) ].flop;
									}
									{toM.flop} */
								
								lvRealMulti.at(morphTo.value - 2).flop.asWFSPointArray;
									};
							});
		
		StaticText.new(lw, Rect(582, 316, 80, 18)).string_("morph");
		
		
		
		selected = SCPopUpMenu.new(lw, Rect(50 + 120, 30, 140, 18))
			.items_(lvNames.copy ++ ['-', if(lvNames.size == 1) {'(all' /*)*/} {'all'}]); 
		selected.value = 0;
		selected.action_({selectMovement.value});
		selected.addUniqueMethod('name', {selected.items.at(selected.value).asSymbol;});
		StaticText.new(lw, Rect(120, 30, 48, 18)).string_("select").setProperty(\align,\right);
		
		restAlpha = SmoothSlider.new(lw, Rect(15, 280, 20, 100)).value_(0.2).step_(0.05); //Rect(350, 472, 100, 18)
		restAlpha.action = {selectMovement.value};
		StaticText.new(lw, Rect(0, 262, 48, 18)).string_("contrast").setProperty(\align,\right);
		
		
		copyMovement = {arg index;
				var newName, addition;
				if (selected.value != lvNames.size)
					{
					addition = $a;
					newName = lvNames.at(index).asString ++ addition;
					
					while { lvNames.any({ |item| item == newName.asSymbol }) }
						{ 
						addition = ( addition.digit + 1 ).asDigit.toLower;
						newName = lvNames.at(index).asString ++ addition;  
						};
						
					lvRealMulti = lvRealMulti.add( lvReal );
					lvNames = lvNames.add( newName.asSymbol );
					lvTimes = lvTimes.add( times );
					
					historySwitch.addItem;
					selected.items_(lvNames.copy ++ ['-','all']);
					selected.valueAction_(lvNames.size - 1);
					fillMorphTo.value;
					colorAgain = true;
					}
				};
		
		removeMovement = {arg index;
				if ((selected.value < lvNames.size) && (lvNames.size != 1))
					{
					lvRealMulti.removeAt(index);
					lvNames.removeAt(index);
					lvTimes.removeAt(index);
					historySwitch.addItem;
					selected.items_(lvNames.copy ++ ['-','all']);
					selected.valueAction_((index-1).max(0));
					fillMorphTo.value;
					colorAgain = true;
					}
					};
					
		renameMovement = {arg index, newName;
				if ( (selected.value < lvNames.size) )
					{
					newName = newName ? lvNames.at(index);
					newName.asString.request({ |theName|
						theName = theName.select({ |item| item != $\n }); //wslib required
						lvNames.put(index, theName.asSymbol );
						historySwitch.addItem;
						selected.items_(lvNames.copy ++ ['-','all']);
						selected.valueAction_(index);
						fillMorphTo.value; }, 
					"Please enter a new name for '" ++ newName ++ "':" );
					//colorAgain = true;
					}
				};
					
		addMovement = {arg positions, times, name, generate; 
						if(name.isNil) {name = "New"};
						lvNames = lvNames.add( (name ++ "_" ++ (lvNames.size + 1) ).asSymbol);
						lvRealMulti = lvRealMulti.add(positions);
						lvTimes = lvTimes.add(times);
						
						historySwitch.addItem;
						selected.items_(lvNames.copy ++ ['-','all']);
						fillMorphTo.value;
						selected.valueAction_(lvNames.size - 1);
						colorAgain = true;
						fitAll.value;
						};
						
		
		
		SCPopUpMenu.new(lw, Rect(152 + 40 + 120, 30, 70, 18))
			.items_(["(actions..",  /*)*/ "-", "generate new..", "draw new..", "-", "duplicate", "remove", "-",
				"rename"
				//, "-", "plotSmooth", "plotSmooth all"
				])
			.action_({ |popUp|
				var def, synth;
				if(popUp.value == 2)
					{ //.value([[-1,1], [-1,1]], [0,1], "gen");
					if(gw.isNil)
						{generateWindow.value}
						{gw.createNew};
					historySwitch.addItem;
					};
				if(popUp.value == 3)
					{ WFSDrawWindow.new };
					
				if(popUp.value == 5)
					{copyMovement.value(selected.value)};
				if(popUp.value == 6)
					{removeMovement.value(selected.value)};
				if(popUp.value == 8)
					{ renameMovement.value(selected.value); };
				if(popUp.value == 10)
					{ this.current.plotSmooth };
				if(popUp.value == 11)
					{ this.paths.plotSmooth };
				popUp.value = 0; });
				
				
				
		generateWindow = { var new = true;///// generate new movement
			var sets, mode, timeBox;
			var polygonViews, waveViews, spiralViews, sineMixViews, randomViews;
			var polygonFunc, waveFunc, spiralFunc, sineMixFunc, randomFunc;
			var currentAction, points, localTimes;
			var update, updateFunc, updateMode;
			//points = [[0],[0]];
			
			currentAction = {if (updateMode.value == 0)
					{updateFunc.value} };
					
			updateFunc = {
				[polygonFunc, waveFunc, spiralFunc, sineMixFunc, randomFunc]
					.at(mode.value).value;
				localTimes = Array.fill(points.at(0).size, { |i|
					(i / (points.at(0).size - 1)) * timeBox.value; });
		
				if(new)
					{addMovement.value(points, localTimes, "Gen");
						new = false};
				originalValue = points;
				originalTimes = localTimes;
				transform.value;
					};
		
			gw = SCWindow.new("generate", Rect(window.bounds.right + 2, 
				window.bounds.top + (window.bounds.height - 120), 250, 120), false);
			//gw.onClose_({ if(lw.notNil) {fix.value}; });
			gw.addUniqueMethod('createNew', {new = true; updateFunc.value});
			mode = SCPopUpMenu.new(gw, Rect(5, 5, 80, 18)).items_(
				['polygon','wave','spiral','lissajous','random']);
				
			update = RoundButton.new(gw, Rect(87, 5, 78, 18)).radius_(0).border_(1).states_([["update"]])
				.action_({updateFunc.value});
				
			updateMode = RoundButton.new(gw, Rect(167, 5, 78, 18))
				.radius_(0).border_(1).states_([["auto-update", Color.gray(0.8), Color.black],				["auto-update", Color.black, Color.clear]]);
				
			sets = Array.fill(mode.items.size, { |i|
				var cp;
				cp = CompositeView.new(gw, Rect(5, 25, 240, 70))
					//.relativeOrigin_(false) /// CHANGE FOR SC 3.2.2
					//.slotPut( \relativeOrigin, false )
					.background_(	
						[Color.black, Color.red, Color.green, 
							Color.yellow, Color.blue ].at(i).alpha_(0.25));
				//cp.decorator = FlowLayout( cp.bounds );
				cp.decorator = AbsLayout( cp );
				cp;
					});
			
			mode.action = {
				sets.do({ |item, i| 
					item.visible_(mode.value == i) });
				currentAction.value;
				};
			
			//polygon
			polygonViews = [
				RoundNumberBox.new(sets.at(0), Rect(10, 30, 38, 18)).value_(3).steps_(1) //nPoints
					.action_({ |nBox| nBox.value = nBox.value.max(2).round(1);
						currentAction.value;}),
				RoundNumberBox.new(sets.at(0), Rect(10, 50, 38, 18)).value_(2).steps_(0.1) //size
					.action_({ |nBox| nBox.value = nBox.value.max(0.1).round(0.1);
						currentAction.value;}),
				RoundNumberBox.new(sets.at(0), Rect(130, 50, 38, 18)).value_(1).steps_(0.05) //odd ratio
					.action_({ |nBox| nBox.value = nBox.value.max(-2).min(4).round(0.05);
						currentAction.value;}),
				SCPopUpMenu.new(sets.at(0), Rect(10, 70, 60, 18))  //open/close
					.items_(["open", "closed"])
					.action_({ |popUp|
						polygonViews.at(0).valueAction = 
							polygonViews.at(0).value + [-1, 1].at(popUp.value);
						})
					];
			polygonViews.do({ |item, i|
				StaticText.new(sets.at(0), Rect(
						item.absoluteBounds.right + 2, item.absoluteBounds.top, 120, 18))
					.string_(["# points", "size (m)", "odd size ratio", "end mode"].at(i));
				});
				
			polygonFunc = {
				var vws; vws = polygonViews;		
				points = Array.fill(vws.at(0).value, { |i| 
					(i / ((vws.at(0).value) + 
						[0, -1].at(vws.at(3).value))) * 2pi });
				points = [points.sin, points.cos] * (vws.at(1).value / 2);
				points = [points.at(0).collect({ |item, i|
							if(i.odd) {item * vws.at(2).value} {item}; }), 
							points.at(1).collect({ |item, i|
							if(i.odd) {item * vws.at(2).value} {item}; })];
				};
				
			//wave
			waveViews = [
				RoundNumberBox.new(sets.at(1), Rect(10, 30, 38, 18)).value_(1).steps_(0.1) //nPeriods
					.action_({ |nBox| nBox.value = nBox.value.max(0.5).round(0.1);
						currentAction.value;}),
				RoundNumberBox.new(sets.at(1), Rect(130, 30, 38, 18)).value_(10).steps_(1) //res
					.action_({ |nBox| nBox.value = nBox.value.max(1).round(1);
						currentAction.value;}),
				RoundNumberBox.new(sets.at(1), Rect(10, 50, 38, 18)).value_(1).steps_(0.1) //size
					.action_({ |nBox| nBox.value = nBox.value.round(0.1);
						currentAction.value;}),
				RoundNumberBox.new(sets.at(1), Rect(130, 50, 38, 18)).value_(1).steps_(0.1) //width
					.action_({ |nBox| nBox.value = nBox.value.round(0.1);
						currentAction.value;}),
				SCPopUpMenu.new(sets.at(1), Rect(10, 70, 60, 18))  //type
					.items_(["sine", "-", "tri", "saw", "square", "-", "vari-saw", "pulse"])
					.action_({ |popUp| 
						if(popUp.value < 6)
							{waveViews.at(1).enabled_(true); waveViews.at(5).enabled_(true); }
							{waveViews.at(1).enabled_(false); waveViews.at(5).enabled_(true);};
							currentAction.value; }),
				RoundNumberBox.new(sets.at(1), Rect(130, 70, 38, 18)).value_(0).steps_(0.1) //phase
					.action_({ |nBox| nBox.value = nBox.value.wrap(0,1).round(0.1);
						currentAction.value;}),
					
					];
					
			waveViews.do({ |item, i|
				StaticText.new(sets.at(1), Rect(
						item.absoluteBounds.right + 2, item.absoluteBounds.top, 120, 18))
					.string_(["# periods", "resolution", "size (m)", 
						"depth (m)", "wave", "phase / width"].at(i));
				});
				
			waveFunc = {
				var vws; 
				var periods, res, size, width, type, phase;
				var pointsX, pointsY;
				vws = waveViews;
				#periods, res, size, width, type, phase =
					vws.collect({|item| item.value});
		
				points = Array.fill((periods * res).ceil.asInt + 1, { |i|
					(i / res)
							});
				
				if (type < 6)
					{points = [(points / periods) * size, 
							[{((points + phase) * 2pi).sin}, //sine
							{},
							{((points + phase).wrap(0.0,1.0) * 4).fold2(1.0)}, //tri discrete
							{((points + phase).wrap(0.0,1.0) * 2) - 1}, // saw discrete
							{(points + phase).collect({ |item| //sqr discrete
								if((item - (1 / (periods * res))).wrap(0.0,1.0) >= 0.5) 
								
											{-1.0} {1.0} }); },
							{}
							].at(type).value
						 * width];	}
					{
					
					if(type == 6) //vari saw
						{pointsX = Array.fill((periods * 2).floor.asInt + 1, 
								{ |i| ([0,phase].wrapAt(i)
									 + (i/2).floor)/ periods});
						pointsY = Array.fill((periods * 2).floor.asInt + 1, 
								{ |i| [1,-1].wrapAt(i) });
							};
				
							
					if(type == 7) //pulse
						{pointsX = 
							Array.fill((periods * 4).floor.asInt + 1, 
								{ |i| ([0,0,phase,phase].wrapAt(i)
									 + (i/4).floor)/ periods});
						pointsY =  Array.fill((periods * 4).floor.asInt + 1, 
								{ |i| [-1,1,1,-1].wrapAt(i) })
							};
							
					points = [pointsX * size, pointsY * width] ;
					};
				};
			
			//spiral
			spiralViews = [
				RoundNumberBox.new(sets.at(2), Rect(10, 30, 38, 18)).value_(5).steps_(0.1) //nPeriods
					.action_({ |nBox| nBox.value = nBox.value.max(0.5).round(0.1);
						currentAction.value;}),
				RoundNumberBox.new(sets.at(2), Rect(130, 30, 38, 18)).value_(10).steps_(1) //res
					.action_({ |nBox| nBox.value = nBox.value.max(1).round(1);
						currentAction.value;}),
				RoundNumberBox.new(sets.at(2), Rect(10, 50, 38, 18)).value_(1).steps_(0.1) //startSize
					.action_({ |nBox| nBox.value = nBox.value.round(0.1);
						currentAction.value;}),
				RoundNumberBox.new(sets.at(2), Rect(10, 70, 38, 18)).value_(0.1).steps_(0.1) //endSize
					.action_({ |nBox| nBox.value = nBox.value.round(0.1);
						currentAction.value;})
					];
					
			spiralViews.do({ |item, i|
				StaticText.new(sets.at(2), Rect(
						item.absoluteBounds.right + 2, item.absoluteBounds.top, 120, 18))
					.string_(["# periods", "resolution", "start size (m)", 
						"end size (m)"].at(i));
				});
				
			spiralFunc = {
				var vws, size, levels; vws = spiralViews;
				size = (vws.at(0).value * vws.at(1).value).ceil.asInt + 1;
				points = Array.fill(size, { |i|
					(i / vws.at(1).value) * 2pi
							});
				levels =  (Array.fill(size, { |i|
					(1 - (i / size))
							}) * (vws.at(2).value - vws.at(3).value)) + vws.at(3).value;
							
				points = [points.sin, points.cos] *.t levels;
				
				};
				
			//sinemix
			sineMixViews = [
				RoundNumberBox.new(sets.at(3), Rect(10, 30, 38, 18)).value_(1).steps_(0.125) //nPeriods x
					.action_({ |nBox| nBox.value = nBox.value.max(0.125).round(0.125);
						currentAction.value;}),
				RoundNumberBox.new(sets.at(3), Rect(130, 30, 38, 18)).value_(2).steps_(0.125) //nPeriods y
					.action_({ |nBox| nBox.value = nBox.value.max(0.125).round(0.125);
						currentAction.value;}),
				RoundNumberBox.new(sets.at(3), Rect(10, 50, 38, 18)).value_(0).steps_(0.025) //phase x
					.action_({ |nBox| nBox.value = nBox.value.wrap(0,1).round(0.025);
						currentAction.value;}),
				RoundNumberBox.new(sets.at(3), Rect(130, 50, 38, 18)).value_(0).steps_(0.025) //phase y
					.action_({ |nBox| nBox.value = nBox.value.wrap(0,1).round(0.025);
						currentAction.value;}),
				RoundNumberBox.new(sets.at(3), Rect(10, 70, 38, 18)).value_(1).steps_(0.1) //size
					.action_({ |nBox| nBox.value = nBox.value.max(0.1).round(0.1);
						currentAction.value;}),
				RoundNumberBox.new(sets.at(3), Rect(130, 70, 38, 18)).value_(10).steps_(1) //npoints
					.action_({ |nBox| nBox.value = nBox.value.max(1).round(1);
						currentAction.value;}),
					];
					
			sineMixViews.do({ |item, i|
				StaticText.new(sets.at(3), Rect(
						item.absoluteBounds.right + 2, item.absoluteBounds.top, 120, 18))
					.string_(["# periods (x)", "# periods (y)",
						"phase (x)", "phase (y)", 
						"size (m)", "# points"].at(i));
				});
				
			sineMixFunc = {
				var vws;
				var periodsX, periodsY, phaseX, phaseY, size, nPoints;
				vws = sineMixViews;
				#periodsX, periodsY, phaseX, phaseY, size, nPoints =
					vws.collect({|item| item.value});
				points = Array.fill(nPoints, { |i|
							i / (nPoints - 1) });
				points = [
					(((points * periodsX) + phaseX) * 2pi).sin,
					(((points * periodsY) + phaseY) * 2pi).sin ] * size;
				};
				
			//random
			randomViews = [
				RoundNumberBox.new(sets.at(4), Rect(10, 30, 38, 18)).value_(5).steps_(1) //nPoints
					.action_({ |nBox| nBox.value = nBox.value.max(1).round(1);
						currentAction.value;}),
				RoundNumberBox.new(sets.at(4), Rect(130, 30, 38, 18)).value_(0).steps_(1) //seed
					.action_({ |nBox| nBox.value = nBox.value.max(0).round(1);
						currentAction.value;}),
				RoundNumberBox.new(sets.at(4), Rect(10, 50, 28, 18)).value_(0).steps_(0.1)
					.action_({ |nBox| nBox.value = nBox.value.round(0.1);
						currentAction.value;}),
				RoundNumberBox.new(sets.at(4), Rect(40, 50, 28, 18)).value_(0).steps_(0.1)			.action_({ |nBox| nBox.value = nBox.value.round(0.1);
						currentAction.value;}),
				RoundNumberBox.new(sets.at(4), Rect(130, 50, 28, 18)).value_(1).steps_(0.1) //max x
					.action_({ |nBox| nBox.value = nBox.value.round(0.1);
						currentAction.value;}),
				RoundNumberBox.new(sets.at(4), Rect(160, 50, 28, 18)).value_(1).steps_(0.1) /*max y*/			.action_({ |nBox| nBox.value = nBox.value.round(0.1); currentAction.value;}),
				SCPopUpMenu.new(sets.at(4), Rect(10, 70, 60, 18))  //type
					.items_(["full", "x only", "y only", "walk"])
					.action_({ |popUp|	currentAction.value; }),
				
				];
					
			randomViews.do({ |item, i|
				StaticText.new(sets.at(4), Rect(
						item.absoluteBounds.right + 2, item.absoluteBounds.top, 120, 18))
					.string_(["# points", "seed", "", "min (x/y)", 
						"", "max (x/y)", "type", "start", "", "dir. (x/y)"].at(i));
				});
				
			randomFunc = {
				var vws;
				var nPoints, seed, minX, minY, maxX, maxY, type, minStep, lastV;
				var min, max;
				var randd, posNeg;
				vws = randomViews;
				#nPoints, seed, minX, minY, maxX, maxY, type =
					vws.collect({|item| item.value});
				randd = Routine.new({ loop({ ((max.asFloat - min.asFloat).rand + min).yield }) });
				randd.randSeed = seed;
				//posNeg = Routine.new({ loop({ [[-1,1].choose, [-1,1].choose].yield }) });
				//posNeg.randSeed = seed + 1;
				min = [minX, minY]; max = [maxX, maxY];
				if(type == 0)
					{points = Array.fill(nPoints, randd).flop; };
				if(type == 1)
					{points = [Array.fill(nPoints, randd).flop.at(0),
							Array.series(nPoints, minY, (maxY - minY) / (nPoints - 1))] };
				if(type == 2)
					{points = [Array.series(nPoints, minX, (maxX - minX) / (nPoints - 1)),
							Array.fill(nPoints, randd).flop.at(1)] };
				if(type == 3)
					{lastV = [0,0];
					points = ([[0,0]] ++ Array.fill(nPoints - 1, {
						var out, newRand;
						newRand = randd.value;
						out = lastV + newRand;
						lastV = out;
						out})).flop / (nPoints.sqrt);
					};
				
				};
		
		
			
			timeBox = RoundNumberBox.new(gw, Rect(45, 97, 38, 18)).value_(1.0).steps_(0.1);
			timeBox.action = { timeBox.value = timeBox.value.max(0.1).round(0.1); currentAction.value; };
			StaticText.new(gw, Rect(5, 97, 40, 18)).string_("dur.");
			
			gw.front;
			mode.action.value;
		
		};
		
		selectMovement = {
				var localValue, localName;		
				removeMultiDisplay.(lsMulti);
				lsMulti = makeMultiDisplay.value(lvRealMulti,lvNames, selected.name);
				lvReal = lvRealMulti.at(selected.value.min(lvRealMulti.size-1));
				times = lvTimes.at(selected.value.min(lvTimes.size-1)).copy;
				originalValue = nil;
				originalMulti = nil;
				originalTimes = nil;
				//if(currentIndex >= lvReal.at(0).size)
				//	{currentIndex = lvReal.at(0).size - 1};
				colorAgain = true;
				transform.value;
				//zoom.value;
				lw.refresh;
				};
				
		transform = {arg presetSelect;
			var center, flipVal, tempint;
			if(presetSelect.isNil)
				{presetSwitch.select(false)}
				{presetSwitch.select(presetSelect)};
					
					
				//fixView.enabled_(true);
					
			if (selected.name != 'all')
					 	
				{	if (originalValue.isNil) {originalValue = lvReal};
					flipVal = [1,1];
					if (flipButtons.at(0).value == 1)
							{flipVal.put(0, -1) };
					if (flipButtons.at(1).value == 1)
							{flipVal.put(1, -1) };
		
					if (repeat.value == 0) 
					{	//center = centerV.(originalValue) ;
						center = [centerBoxes.at(0).value, centerBoxes.at(1).value];
						lvReal = WFSPath(originalValue.flop, name: 'temp')
							.transform( [move[0].value, move[1].value], 
								[scale2Dboxes.at(0).value, scale2Dboxes.at(1).value] * flipVal,
								rotate.value * 360.0,  [false, true].at(reverse.value), center: center)
							.round(quantize.value).asXYArray.flop;
						}
						
					{ lvReal = WFSPath(originalValue.flop, name: 'temp').
							dupTransform(repeat.value + 1, [0,0], 
								[scale2Dboxes.at(0).value, scale2Dboxes.at(1).value] * flipVal,
								rotate.value * 360.0, [false, true].at(reverse.value) )
							.glue.move([move[0].value, move[1].value])
							.round(quantize.value).asXYArray.flop 
						};
							
					// interpolation
					
					case {  interpolateBox.value == 1 }
						{ nil  }
						{  intType.wrapAt(0) == 'hermite_p' }
						{ 	
							tempint = lvReal[0].collect({ |item, i|
								var point;
								point = Point( item, lvReal[1][i] );
								[ point.rho, point.theta ]
								}).flop;
								
							tempint.postln;
								
							tempint = [	
								tempint.at(0).interpolate( interpolateBox.value, 'hermite',
									[false, true].at(intCloseOpen.value)),
								tempint.at(1).interpolate( interpolateBox.value, 'hermite',
									[false, true].at(intCloseOpen.value)) ];
									
							lvReal = tempint[0].collect({ |item, i|
								var point;
								point = Polar( item, tempint[1][i] );
								[ point.real, point.imag]
								}).flop;
								
						}
						{ true }
						{	lvReal = [	lvReal.at(0).interpolate( interpolateBox.value, intType.wrapAt(0),
										[false, true].at(intCloseOpen.value), 
											(if( intType.wrapAt(0) == 'spline' ) { intCurve } { nil }) ),
								lvReal.at(1).interpolate( interpolateBox.value, intType.wrapAt(1),
										[false, true].at(intCloseOpen.value),
											(if( intType.wrapAt(1) == 'spline' ) { intCurve } { nil })) ];
						};
		
					// morph
		
					if(morph.value != 0.0, {lvReal = 
						lvReal.flop.asWFSPointArray.blend(
							morphTo.getM, 
							morph.value).asXYArray.flop});
		
			
				}
				{	///case 'all'
					if (originalMulti.isNil) {originalMulti = lvRealMulti};
					flipVal = [1,1];
					if (flipButtons.at(0).value == 1)
							{flipVal.put(0, -1) };
					if (flipButtons.at(1).value == 1)
							{flipVal.put(1, -1) };
					
				lvRealMulti = originalMulti.collect({arg originalX, iX;
					var lvRealOut;
					if (repeat.value == 0) 
					{	center = centerV.(originalX) ;
						lvRealOut = WFSPath(originalX.copy.flop, name: 'temp')
							.transform( [move[0].value, move[1].value], 
								[scale2Dboxes.at(0).value, scale2Dboxes.at(1).value] * flipVal,
								rotate.value * 360.0,  [false, true].at(reverse.value), center: center)
							.round(quantize.value).asXYArray.flop;
						
						}
					{	
						 lvRealOut = WFSPath(originalValue.flop, name: 'temp')
							.dupTransform( repeat.value + 1, [0,0], 
								[scale2Dboxes.at(0).value, scale2Dboxes.at(1).value] * flipVal,
								rotate.value * 360.0, [false, true].at(reverse.value) )
							.glue.move([move[0].value, move[1].value])
							.round(quantize.value).asXYArray.flop 
					};
					
					lvRealOut = [	lvRealOut.at(0).interpolate( interpolateBox.value, intType.wrapAt(0),
									[false, true].at(intCloseOpen.value), intCurve),
								lvRealOut.at(1).interpolate( interpolateBox.value, intType.wrapAt(1),
									[false, true].at(intCloseOpen.value), intCurve) ];					
					if(morph.value != 0.0, {lvRealOut = 
						lvRealOut.flop.asWFSPointArray.blend(morphTo.getM, morph.value).asXYArray.flop});
							
					lvRealOut;
					};);
					
					lvReal = lvRealMulti.at(selected.value.min(lvRealMulti.size-1));
					
					
				};
					if(currentIndex >= lvReal.at(0).size)
					{currentIndex = lvReal.at(0).size - 1};
					transformTime.value;
					zoom.value;
					};
		
		fix = { //fix all transformations and reset sliders
			var center;
			usePreset.value(resetPreset);
			//originalValue = lvReal; //actual fix
			originalValue = nil;
			fixTime.value;
			if(selected.name != 'all')
				{lvRealMulti.put(selected.value, lvReal);}
				{originalMulti = lvRealMulti};
			historySwitch.addItem;
			zoom.value;
			fixView.enabled_(false);
			if(centerMode.value == 1)
			{center = centerV.(lvReal);
				centerBoxes.at(0).value = center.at(0); centerBoxes.at(1).value = center.at(1);};
			};
			
		fixView = RoundButton.new(lw, Rect(480, 382, 128, 18));
		fixView.radius_(0).border_(1).states_([["keep changes", Color.black, Color.red.alpha_(0.33) ]]);
		fixView.action = {
			fixView.radius_(0).border_(1).states_([["keep changes", Color.black, Color.red.alpha_(0.33) ]]);
			fix.value};
			
		fixView.view.addUniqueMethod('makeRed', { 
			if(fixView.states.at(0).at(1).asArray != Color.red.asArray)
					{fix.value;
					fixView.radius_(0).border_(1).states_([["keep changes", Color.red, Color.red.alpha_(0.25) ]]);
					fixView.enabled_(true);
					};
					lw.refresh;
				});
			
		fixView.enabled_(false);
					
		/*
		lb = RoundButton.new(lw, Rect(500, 372, 80, 18)); //timed random walk
		lbSize = RoundNumberBox.new(lw, Rect(582, 372, 45, 18));
		lbSize.value = 0.05; lbSize.steps_(0.01);
		lbSize.action_({|numbox| if(numbox.value < 0.01) {numbox.value = 0.01}; });
		lb.states = [["randomwalk"], ["stop"]];
		lb.action = { AppClock.sched(0.1, {lvReal = ([Array.rand2(lvReal.at(0).size, lbSize.value) + lvReal.at(0),
							 Array.rand2(lvReal.at(1).size, lbSize.value) + lvReal.at(1)] );
			originalValue = lvReal;
			zoom.value;
			if(lb.value == 1) {0.1;} {nil}
			}); };
		*/
			
		//times
			
		timeScale = RoundNumberBox.new(lw, Rect(540, tvOffset + 144, 40, 18)).value_(1.0).steps_(0.1);
			timeScale.action = {
				timeScale.value = timeScale.value.max(0.1);
				timeGrid.fill; 
				timeFill.value('no fit');
				};
				
		StaticText.new(lw, Rect(582, tvOffset + 144, 60, 18)).string_("s");
			
		RoundButton.new(lw, Rect(498, tvOffset + 144, 40, 18)).radius_(0).border_(1).states_([["fit"]]).action_({
				timeScale.valueAction = ((times.last * 10).ceil / 10).max(0.1); 
				});
		
		StaticText.new(lw, Rect(0, tvOffset + 33 , 48, 48))
			.string_("times &\n speeds").stringColor_(Color.black.alpha_(0.75))
			.setProperty( \align, \right );
	
		multipleText = StaticText.new(lw, Rect(50, tvOffset + 40, 530, 100));
		multipleText.setProperty(\align, \center);
		multipleText.font_(Font("Helvetica-Bold", 30));
		multipleText.stringColor_(Color.black.alpha_(0.33));
		multipleText.string_("multiple selection");
		multipleText.visible_("false");
	
		timeGrid = SCEnvelopeView.new(lw, Rect(53, tvOffset + 40, 523, 100));
		timeGrid.setProperty(\thumbSize, [-1, 1]);
		timeGrid.setProperty(\thumbHeight, [-1,100]);
		timeGrid.strokeColor_(Color.clear);
		timeGrid.addUniqueMethod('fill', {arg view;
				var colors;
				var divColor, halfSecondsColor, secondsColor, tenSecondsColor, minutesColor, tenMinutesColor;
				divColor = Color.white.alpha_(0.8 * (1 - (timeScale.value / 10.0)).max(0.0));
				halfSecondsColor = Color.gray.alpha_(0.8 * (1 - (timeScale.value / 60.0)).max(0.0));
				secondsColor = Color.red.alpha_(0.8 * (1 - (timeScale.value / 120.0)).max(0.0));
				tenSecondsColor = Color.blue.alpha_(0.8 * (1 - (timeScale.value / 1200.0)).max(0.0));
				minutesColor = Color.green.alpha_(0.8);
				tenMinutesColor = Color.yellow.alpha_(0.8);
				
				if(timeScale.value <= 10)
					{	timeGrid.value_([Array.fill((timeScale.value * 10.0).floor.asInt,
							{ |i| i / (timeScale.value * 10.0) ; }),
							[0.5].stutter((timeScale.value * 10.0).floor.asInt)] );
						colors = 	[secondsColor] ++ [divColor].stutter(4) ++ 
								[halfSecondsColor] ++ [divColor].stutter(4);
						timeGrid.value.at(0).size.do({ |i|
						//timeGrid.select(i);
						timeGrid.setFillColor(i, colors.wrapAt(i));
						});
					};
				
				if((timeScale.value > 10) && (timeScale.value <= 60))
					{	timeGrid.value_([Array.fill((timeScale.value * 2.0).floor.asInt,
							{ |i| i / (timeScale.value * 2.0) ; }),
							[0.5].stutter((timeScale.value * 2.0).floor.asInt)] );
						colors = [tenSecondsColor, halfSecondsColor] ++ 
								[[secondsColor, halfSecondsColor]].stutter(9).flat;
						timeGrid.value.at(0).size.do({ |i|
						//timeGrid.select(i);
						timeGrid.setFillColor(i, colors.wrapAt(i));
						});
					};
					
				if((timeScale.value > 60) && (timeScale.value <= 120))
					{	timeGrid.value_([Array.fill((timeScale.value).floor.asInt,
							{ |i| i / (timeScale.value) ; }),
							[0.5].stutter((timeScale.value).floor.asInt)] );
						colors = [minutesColor] ++ [secondsColor].stutter(9) ++ 
								[[tenSecondsColor] ++ [secondsColor].stutter(9)].stutter(5).flat;
						timeGrid.value.at(0).size.do({ |i|
						//timeGrid.select(i);
						timeGrid.setFillColor(i, colors.wrapAt(i));					});
					};
				
				if(timeScale.value > 120)
					{	timeGrid.value_([Array.fill((timeScale.value / 10.0).floor.asInt,
							{ |i| i / (timeScale.value / 10.0) ; }),
							[0.5].stutter((timeScale.value / 10.0).floor.asInt)] );
						colors = [tenMinutesColor] ++ [tenSecondsColor].stutter(5) ++
								[[minutesColor] ++ [tenSecondsColor].stutter(5)].stutter(9).flat;
						timeGrid.value.at(0).size.do({ |i|
						//timeGrid.select(i);
						timeGrid.setFillColor(i, colors.wrapAt(i));
						});
					};
				
					});
			
		timeGrid.fill;
		timeSelectView = SCEnvelopeView(lw, Rect(50, tvOffset + 40, 530, 100)) 
			.drawLines_(false)
			.drawRects_(true);
		timeSelectView.setProperty(\thumbSize, [-1,7]); 
		timeSelectView.setProperty(\thumbHeight, [-1,7]); 
		timeSelectView.setProperty(\fillColor, [-1, Color.yellow.alpha_(0.8)]);
		timeSelectView.setProperty(\strokeColor, Color.clear);
		timeSelectView.value_([0.5,0.5].collect({ |item| [item] }););
		
		timeBack = SCEnvelopeView.new(lw, Rect(50, tvOffset + 40, 530, 100));
		timeBack.drawRects_(false).drawLines_(true);
		//timeBack.strokeColor_(Color.red);
		timeBack.setProperty(\thumbSize, [-1, 7]);
		timeBack.setProperty(\thumbHeight, [-1, 7]);
		timeView = SCEnvelopeView.new(lw, Rect(50, tvOffset + 40, 530, 100));
		timeView.drawLines_(false);
		timeView.setProperty(\thumbSize, [-1, 7]);
		timeView.setProperty(\thumbHeight, [-1, 7]);
		timeView.action_({
			var index;
			currentIndex = timeView.index;
			fixTime.value;
			timeView.value = [timeView.value.at(0).sort.collect({|item, i|
						if(i == 0) {0.0} {item}; }), 
			timeView.value.at(1).collect({ |item, i|
						if(i == (timeView.value.at(1).size-1)) {0.0} {item}; })];
							timeFill.value(nil, timeView.value.at(0));
			
			selectView.value_(ls.value.copy.flop.at(currentIndex).collect({ |item| [item] }));
			
			});
			
		timeView.keyDownAction = { arg view,char,modifiers,unicode,keycode;
			if ((unicode == 16rF700) or: (unicode == 16rF703), { 
				//view.select((view.index + 1).wrap(0, view.value.at(0).size - 1));
				currentIndex = ((currentIndex + 1).wrap(0, lvReal.at(0).size - 1));
				});
			if ((unicode == 16rF701) or: (unicode == 16rF702), { 
				//view.select((view.index - 1).wrap(0, view.value.at(0).size - 1)); 
				currentIndex = ((currentIndex - 1).wrap(0, lvReal.at(0).size - 1));
				});
			ls.action.value;
	
			};
			
		timeString = StaticText.new(lw, Rect(288, 0, 220, 46));
		timeString.addUniqueMethod('updatePos', {
			var az, currentPoint, currentTime;
			currentIndex = currentIndex.max(0);
			
			if (lvReal.at(0).size != nPoint.items.size)
				{nPoint.items_(Array.fill(lvReal.at(0).size, {|i| "point " ++ (i + 1)})) };
			currentPoint = [lvReal.at(0).at(currentIndex), lvReal.at(1).at(currentIndex)] 
					- [[0,0], 
						lvReal.copy.flop.wrapAt(currentIndex -1), 
						lvReal.copy.flop.wrapAt(currentIndex +1)].at(nRel.value);
			currentTime = times.at(currentIndex) - 
					[0, 
					times.wrapAt(currentIndex -1), 
					times.wrapAt(currentIndex +1)].at(nRel.value);
			nPoint.value = currentIndex;
			nX.value = currentPoint.at(0);
			nY.value = currentPoint.at(1);
			az = WFSTools.xyToAZ([currentPoint])[0];
			nDist.value = az.at(1);
			nAngle.value = az.at(0);
			nTime.value = currentTime;
			nSpeed.value =  ((((lvReal.at(0).wrapAt(currentIndex + 1) - lvReal.at(0).at(currentIndex)) ** 2) +
					  ((lvReal.at(1).wrapAt(currentIndex + 1) - lvReal.at(1).at(currentIndex)) ** 2))**0.5) /
					  (times.clipAt(currentIndex + 1) - times.at(currentIndex));
			
				});
		
		centerBoxes = [RoundNumberBox.new(lw, Rect(500, centerOffset, 38, 18))
						.value_(0.0).steps_(0.1)
						.action_({centerMode.value = 3;
								transform.value}),
					RoundNumberBox.new(lw, Rect(540, centerOffset, 38, 18))
						.value_(0.0).steps_(0.1)
						.action_({centerMode.value = 3;
								transform.value})];
		
		centerMode = SCPopUpMenu.new(lw, Rect(500, centerOffset + 20, 80, 18))
					.items_(["absolute", "path", "selected point..", "(costum" /*)*/ ]);
		
		centerMode.action_({
			var center;
			if(centerMode.value == 0)
				{center = [0.0, 0.0];};
			if(centerMode.value == 1)
				{if(originalValue.isNil) {originalValue = lvReal};
					center = centerV.(originalValue);
				};
			if(centerMode.value == 2)
				{//centerBoxes.at(0).value = nX.value; centerBoxes.at(1).value = nY.value;
					center = lvReal.copy.flop.at(currentIndex.max(0));
					centerMode.value = 3};
			centerBoxes.at(0).value = center.at(0); centerBoxes.at(1).value = center.at(1);
			transform.value;
			});
					
		StaticText.new(lw, Rect(582, centerOffset, 80, 18)).string_("center (x/y)");
		StaticText.new(lw, Rect(582, centerOffset + 20, 80, 18)).string_("center mode");
		
		/////// POINTS
		
		numEdit = SCCompositeView(lw,Rect(50, 494, 442 ,44));  //400
		numEdit.background = Color.gray(0.5).alpha_(0.5);
		//numEdit.slotPut( \relativeOrigin, false  ); // CHANGE BEFORE 3.4!!
		//numEdit.relativeOrigin = false;
		numEdit.decorator = AbsLayout( numEdit );
		
		RoundButton.new(lw, 
				Rect(452, 494 + 2, 38, 18))
			.radius_(0).border_(1).states_([["edit"],["lock"]])
			.action_({ |button|
					if(button.value == 1)
					{numEdit.enabled_(true)}
					{numEdit.enabled_(false)};
					});
					
		
		addPoint = SCPopUpMenu.new(lw, Rect(370, 496, 80, 18)); //Rect(410, 496, 38, 18)
		
		addPoint.addUniqueMethod('rebuild', {
				addPoint.items = ["(add point..", /*)*/ "-", 
						"after selected point", 
						"before selected point", 
						"-",
						"after end point", 
						"before first point",
						"-",
						"cut selected point",
						"copy selected point",
						if(clipboardPoint.isNil)
							{"(empty clipboard" /*)*/}
							{"empty clipboard"}
						];
						});
		
		addPoint.rebuild;
						
		addPoint.action = {
			var newlvReal, newPoint, newTime;
			
			if(addPoint.value == 2) //after selected point
				{newPoint = clipboardPoint ? ((lvReal.copy.flop.at(currentIndex) + 
					lvReal.copy.flop.wrapAt(currentIndex + 1)) / 2);
				newTime = (times.at(currentIndex) + 
							(times ++ [times.last + (times.last - times.at(times.size-2))]).at(currentIndex + 1)) 
							/ 2;
					lvReal = lvReal.copy.flop.insert(currentIndex + 1, newPoint).flop; 
					times = times.insert(currentIndex + 1, newTime);
					currentIndex = currentIndex + 1;
					zoom.value;};
					
			if(addPoint.value == 3) //before selected point
				{newPoint = clipboardPoint ? ((lvReal.copy.flop.at(currentIndex) + 
					lvReal.copy.flop.wrapAt(currentIndex - 1)) / 2);
					lvReal = lvReal.copy.flop.insert(currentIndex, newPoint).flop; 
					if(currentIndex == 0)
						{times = [times.first] ++ (times + ((times.at(1) - times.first) / 2));}
						{times = times.insert(currentIndex, 
							(times.at(currentIndex) + 
							times.at(currentIndex - 1)) / 2);};
					zoom.value;};
					
			if(addPoint.value == 5) //after end point
				{newPoint = clipboardPoint ? (lvReal.copy.flop.last + 
					(lvReal.copy.flop.last - lvReal.copy.flop.at(lvReal.copy.flop.size -2)));
				newTime = times.last + (times.last - times.at(times.size - 2));
					lvReal = lvReal.copy.flop.add(newPoint).flop; 
					times = times.add(newTime);
					//currentIndex = currentIndex + 1;
					zoom.value;};
		
					if(addPoint.value == 6) //before first point
				{newPoint = clipboardPoint ? (lvReal.copy.flop.first + (lvReal.copy.flop.first - lvReal.copy.flop.at(1)));
					lvReal = lvReal.copy.flop.insert(0, newPoint).flop; 
					times = [times.first] ++ (times + ((times.at(1) - times.first)));			zoom.value;};
					
			if(addPoint.value == 8) //cut
				{newlvReal = lvReal.copy.flop;
				clipboardPoint = newlvReal.removeAt(currentIndex);
				lvReal = newlvReal.flop;
				times.removeAt(currentIndex);
				zoom.value;
				};
				
			if(addPoint.value == 9) //copy
				{clipboardPoint = lvReal.copy.flop.at(currentIndex);};
		
				
			if(addPoint.value == 10) //empty clipboard
				{clipboardPoint = nil;};
				
		
			//fixView.makeRed;
			fixView.action.value; 
			addPoint.rebuild;
			addPoint.value = 0;
			};
			
		// points
		
		
		StaticText.new(lw, Rect(0, 494, 48, 20))
			.string_("points").stringColor_(Color.black.alpha_(0.75))
			.setProperty( \align, \right );
		
		nPoint = SCPopUpMenu.new(lw,
				Rect(50 + 40, 494 + 5, 60, 15))
			.items_(Array.fill(10, {|i| "point " ++ (i + 1)})).font_(Font("Arial", 10));
			
		nRel = SCPopUpMenu.new(lw,
				Rect(50 + 5, 494 + 25, 95, 15))
			.items_(["absolute", "from prev. point", 
					"from next point"]).font_(Font("Arial", 10));
					
		StaticText.new(lw, 
				Rect(50 + 140, 494+ 5, 40, 15))
			.string_("x").font_(Font("Arial", 10));
		
		StaticText.new(lw, 
				Rect(50 + 140, 494 + 25, 40, 15))
			.string_("y").font_(Font("Arial", 10));
			
		StaticText.new(lw, 
				Rect(50 + 0, 494 + 5, 37, 15))
			.string_("select").font_(Font("Arial", 10)).setProperty(\align, \right);
			
		StaticText.new(lw, 
				Rect(50 + 192, 494 + 5, 80, 15))
			.string_("distance").font_(Font("Arial", 10));
			
		StaticText.new(lw, 
				Rect(50 + 192, 494 + 25, 80, 15))
			.string_("angle").font_(Font("Arial", 10));
			
		StaticText.new(lw, 
				Rect(50 + 277, 494 + 5, 80, 15))
			.string_("time").font_(Font("Arial", 10));
			
		StaticText.new(lw, 
				Rect(50 + 277, 494 + 25, 160, 15))
			.string_("speed (to next point)").font_(Font("Arial", 10));
					
		
		
		nX = RoundNumberBox.new(numEdit, 
				Rect(numEdit.bounds.left + 102, numEdit.bounds.top + 5, 35, 15))
			.value_(0.0).steps_(0.1).font_(Font("Arial", 10)); 
			
		nY = RoundNumberBox.new(numEdit, 
				Rect(numEdit.bounds.left + 102, numEdit.bounds.top + 25, 35, 15))
			.value_(0.0).steps_(0.1).font_(Font("Arial", 10)); 
		
		nDist = RoundNumberBox.new(numEdit, 
				Rect(numEdit.bounds.left + 155, numEdit.bounds.top + 5, 35, 15))
			.value_(0.0).steps_(0.1).font_(Font("Arial", 10)); 
			
		nAngle = RoundNumberBox.new(numEdit, 
				Rect(numEdit.bounds.left + 155, numEdit.bounds.top + 25, 35, 15))
			.value_(0.0).steps_(1.0).font_(Font("Arial", 10)); 
			
		nTime = RoundNumberBox.new(numEdit, 
				Rect(numEdit.bounds.left + 240, numEdit.bounds.top + 5, 35, 15))
			.value_(0.0).steps_(0.001).font_(Font("Arial", 10)); 
			
		nSpeed = RoundNumberBox.new(numEdit, 
				Rect(numEdit.bounds.left + 240, numEdit.bounds.top + 25, 35, 15))
			.value_(0.0).steps_(0.1).font_(Font("Arial", 10)); 
					
		numEdit.enabled_(false);
		
		nPoint.action = {currentIndex = nPoint.value; timeString.updatePos; zoom.value;};
		nRel.action = {timeString.updatePos};
		nX.action = {fixView.makeRed;
					lvReal.at(0).put(currentIndex, 
						[0, lvReal.copy.at(0).wrapAt(currentIndex -1), 
							lvReal.copy.at(0).wrapAt(currentIndex +1)].at(nRel.value) +
						nX.value);
					zoom.value; };
		nY.action = {fixView.makeRed;
					lvReal.at(1).put(currentIndex, 
						[0, lvReal.copy.at(1).wrapAt(currentIndex -1), 
							lvReal.copy.at(1).wrapAt(currentIndex +1)].at(nRel.value) +
						nY.value);
					zoom.value; };
		nTime.action = {fixView.makeRed;
					if(nPoint.value != 0)
						{times.put(currentIndex, nTime.value + [0, 
						times.wrapAt(currentIndex -1), 
						times.wrapAt(currentIndex +1)].at(nRel.value)); times = times.sort;};
					zoom.value;
					};
		nDist.action = {
					var newPoint;
					fixView.makeRed;
					newPoint = WFSTools.singleAZToXY(nAngle.value, nDist.value) + [[0,0], 
							lvReal.copy.flop.wrapAt(currentIndex -1), 
							lvReal.copy.flop.wrapAt(currentIndex +1)].at(nRel.value);
					lvReal = lvReal.copy.flop.put(currentIndex, newPoint).flop;
					zoom.value;
					};
		nAngle.action = nDist.action;
					
		
		
			speedScale = RoundNumberBox.new(lw, Rect(582, tvOffset + 40, 40, 18)).value_(1.0).steps_(0.1);
		speedScale.action_({ |nBox|
				speedScale.value_(speedScale.value.max(0.1));
				timeFill.value(speedScale.value)});
		
		StaticText.new(lw, Rect(624, tvOffset + 40, 80, 18)).string_("m/s");
		
		RoundButton.new(lw, Rect(582, tvOffset + 60, 40, 18)).radius_(0).border_(1).states_([["fit"]]).action_({
				speedScale.value = timeFill.value.maxItem;
				timeFill.value;});
			
		RoundButton.new(lw, Rect(582, tvOffset + 80, 40, 18)).radius_(0).border_(1).states_([["lock"],["locked", Color.red]]).action_({
				|button| if(button.value == 1) {fixTime.value; lockSpeeds = true;} {fixTime.value; lockSpeeds = false};
				timeFill.value;});	
					
					
		scaleTime = [SmoothSlider.new(lw, Rect(50, tvOffset + 144, 100, 18)).value_(0.5)
						.action_({
							scaleTime.at(1).value = (scaleTime.at(0).value * 2).max(0.05);
							transformTime.value;
							}),
					RoundNumberBox.new(lw, Rect(152, tvOffset + 144, 40, 18)).value_(1.0)
						.clipLo_( 0.05 )
						.action_({
							scaleTime.at(0).value = (scaleTime.at(1).value / 2).max(0.05);
							transformTime.value})];
							
		StaticText.new(lw, Rect(0, tvOffset + 144, 48, 18))
			.string_("scale").setProperty(\align,\right);
			
			
		StaticText( lw,  Rect(485, tvOffset + 164, 50, 18))
			.string_( "duration" ).align_( \right );
			
		durationView = RoundNumberBox( lw,  Rect(540, tvOffset + 164, 40, 18))
			.value_(0.0)
			.enabled_( false ); // do later...
			
		StaticText.new(lw, Rect(582, tvOffset + 164, 60, 18)).string_("s");
			
		
		quantizeTime = [SmoothSlider.new(lw, Rect(50, tvOffset + 166, 100, 18)).value_(0.0).step_(0.1)
							.action_({transformTime.value}),
						RoundNumberBox.new(lw, Rect(152, tvOffset + 166, 40, 18)).value_(0.0).steps_(0.1)
							.action_({ |box| box.value = box.value.max(0).round(0.1);
									transformTime.value })];
		
		StaticText.new(lw, Rect(0, tvOffset + 166, 48, 18)).string_("quantize").setProperty(\align,\right);
					
		
		warpTime = RoundNumberBox.new(lw, Rect(390, tvOffset + 144, 40, 18)).value_(0.0).steps_(0.5);
		warpTime.action = {transformTime.value};
		
		StaticText.new(lw, Rect(432, tvOffset + 144, 100, 18)).string_("warp");
		
		equalTime = [SmoothSlider.new(lw, Rect(220, tvOffset + 144, 80, 18)).value_(0).step_(0.1)
						.action_({
							if( (equalTime.at(0).value + equalTime.at(1).value) > 1.0)
								{equalTime.at(1).value = 1.0 - equalTime.at(0).value};
							transformTime.value; }),
					SmoothSlider.new(lw, Rect(220, tvOffset + 166, 80, 18)).value_(0).step_(0.1)
						.action_({
							if( (equalTime.at(0).value + equalTime.at(1).value) > 1.0)
								{equalTime.at(0).value = 1.0 - equalTime.at(1).value};
							transformTime.value; })];
				
		StaticText.new(lw, Rect(302, tvOffset + 144, 120, 18)).string_("equal times");
		StaticText.new(lw, Rect(302, tvOffset + 166, 120, 18)).string_("equal speeds");
							
		
		transformTime = {
			var warp, eqlTimes, eqlSpeeds, distances, avgSpeed, newDeltaTimes;
			var tempTimes, eqlTimeValues;
			if(originalTimes.isNil)
				{originalTimes = times};
				
			fixView.enabled_(true);
			warp = CurveWarp.new(ControlSpec.new(0.0, originalTimes.last), warpTime.value);
			
			times = warp.map(originalTimes / originalTimes.last );
			
			if(repeat.value == 0)
				{times = times * scaleTime.at(1).value;}
				{tempTimes = times;
				repeat.value.do({ |i|
						times = times ++ ((tempTimes[1..] * (scaleTime.at(1).value**(i+1))) + times.last);
						});
				};
			
			times = times.interpolate( interpolateBox.value, intType.wrapAt(2), false, intCurve);
								
			/* if(intCloseOpen.value == 1)
				{times = times ++ [times.last + times.at(1)]}; */
								
			times = times.sort;
			
			if ((quantizeTime.at(0).value != 0) && (quantizeTime.at(1).value != 0))
				{	times = ((1 - quantizeTime.at(0).value) * times) + 
					(quantizeTime.at(0).value * times.round(quantizeTime.at(1).value)); };
			
			eqlTimeValues = [ equalTime.at(0).value, equalTime.at(1).value ]; 
			
			if( eqlTimeValues != [0,0] )
				{	
					eqlTimes = times.collect({ |item, i|
							(i / (times.size - 1)) * times.last });
					
					distances = WFSPath(lvReal.flop, name: 'temp').distances;
					avgSpeed = (distances.sum / times.last);
					newDeltaTimes = distances / avgSpeed;
					eqlSpeeds = [times.first];
					newDeltaTimes.size;
					(times.size - 1).do({ |i|
							eqlSpeeds = eqlSpeeds.add(eqlSpeeds.last + newDeltaTimes.at(i));
							});
						
				 times = 	(eqlTimeValues.at(0) * eqlTimes) + 
						(eqlTimeValues.at(1) * eqlSpeeds) + 
						( (1 - eqlTimeValues.sum ) * times); 
				
				};
			
			//times = WFSTools.validateTimeLine(times);
						
			timeFill.value
			};
			
		fixTime = {
			originalTimes = nil;
			lvTimes.put(selected.value, times);
			scaleTime.at(0).value = 0.5;
			scaleTime.at(1).value = 1;
			warpTime.value = 0;
			quantizeTime.at(0).value = 0;
			quantizeTime.at(1).value = 0;
			equalTime.at(0).value = 0;
			equalTime.at(1).value = 0;
			};
			
		timeFill = { |scale, newTimes|
				var speeds, distances, newDeltaTimes;
				if (times.size != lvReal.at(0).size) //dirty but helpful
					{times = Array.fill(lvReal.at(0).size, {|i| times.wrapAt(i)}); };
			if (selected.name != 'all')
			{
				multipleText.visible_(false);
				if (lockSpeeds)
				{
				if(lastSpeeds.isNil)
					{lastSpeeds = WFSPath(lvReal.flop, name: 'temp').timeLine_(times).speeds ++ [0.0];};
				if (newTimes.notNil)
					{ lastSpeeds = lastSpeeds.collect({ |item, i|
						if ((item / speedScale.value) > 1.0)
							{item}
							{timeView.value.at(1).at(i) * speedScale.value}
							});
						}; 
					
				distances = WFSPath(lvReal.flop, name: 'temp').distances; 
				newDeltaTimes = distances / lastSpeeds;
				times = [times.first];
				distances.size.do({ |i|
								times = times.add(times.last + newDeltaTimes.at(i));
								});
				speeds = lastSpeeds;
				}
				{
				if (newTimes.notNil)
					{ times = times.collect({ |item, i|
						if ((item / timeScale.value) > 1.0)
							{item}
							{newTimes.at(i) * timeScale.value}
							});
						};
						
				speeds = WFSPath(lvReal.flop, name: 'temp').timeLine_(times).speeds ++ [0.0];
				
				};
				
				lastSpeeds = speeds;
				
				if(scale.isNil)
					{if(speeds.maxItem > speedScale.value)
						{speedScale.value = speeds.maxItem.min(100000); };
					speeds = speeds / speedScale.value;
					if(speeds.maxItem < 0.1)
						{speedScale.value = (1 / speeds.maxItem) * 0.8; };			if((times.last > timeScale.value) or: (times.last < (timeScale.value * 0.1)))
					{timeScale.value_(((times.last * 10).ceil / 10).max(0.1)); 
					timeGrid.fill;};
					}
					{speeds = speeds / speedScale.value; };
				
				timeView.value_( [times / timeScale.value, speeds]); 
				timeBackFill.value;
				timeString.updatePos;
				timeSelectView.value_(timeView.value.copy.flop.at(currentIndex).collect({ |item| [item] }));
				durationView.value = times.last;
				speeds;
			}
			{multipleText.visible_(true);
			timeView.value_([[],[]]);
			timeBack.value_([[],[]]);
			timeSelectView.value_([[],[]]);
			}
					};
					
			timeBackFill = {
				timeBack.value_(
					[timeView.value.at(0).stutter(2)[1..].add(timeView.value.at(0).last),
					timeView.value.at(1).stutter(2)]) };
					
			timeFill.value;
			lw.addUniqueMethod('interpolate', {arg me, div = 2, type = 'linear';
							times = times.interpolate(div, type, false);
							ls.value_([ls.value.at(0).interpolate(div, type, false),
									ls.value.at(1).interpolate.(div, type, false)]);
							timeFill.value(nil, times); });
			
		SCPopUpMenu( lw, Rect( 10, 0, 50,18 ) )
			.items_( [ "(file", /*)*/ "-", "open..",
				"-", "save selected path as..", "save all paths as..", 
				"-", "import SVG..", "export SVG.." , "export EPS.."] )
			.action_( { |popUp|
			
				var positionsLocal, timesLocal, nameLocal;
				var movLocal, lastTime = 0;
				var allMovs;
				
				case { popUp.value == 2 } // open ( pathBank or scoreFile )
					{  CocoaDialog.getPaths(
						{arg paths, pathArray;
							pathArray = WFSPathArray.readWFSFile(paths.first);
							SCAlert( "% paths found in file '%'"
									.format( pathArray.size, paths.first.basename ), 
										[ "cancel", "add", "replace all" ],
										[ { }, { pathArray.do({
											|item| addWFSPath.value(item); }) }, 
											{ WFSPathEditor( pathArray ); } ] ); 
							}, { "cancelled".postln; });					}
					{ popUp.value == 4 } // save current as pathBank
					{ 	positionsLocal = lvReal.flop;
						timesLocal = times;
						timesLocal = timesLocal.collect({ |item| 
							var out;
							out = item - lastTime;
							lastTime = item;
							out;
							});
						timesLocal = timesLocal[1..];
						nameLocal = lvNames[selected.value];
						movLocal = WFSPath( positionsLocal, timesLocal, nameLocal );
					 	CocoaDialog.savePanel(
								{ |path| movLocal.asWFSPathArray.writeWFSFile( path ); }, 								{ movLocal.asWFSPathArray.writeWFSFile( nil ).postln; } 
							);
					}
					{ popUp.value == 5 } // save all as pathBank
					{	allMovs = [];
						lvRealMulti.do({ |item, i|
							var timesLocal2;
							timesLocal2 = lvTimes[i];
							timesLocal2 = timesLocal2.collect({ |item| 
								var out;
								out = item - lastTime;
								lastTime = item;
								out;
								});
							timesLocal2 = timesLocal2[1..];  
							allMovs = allMovs.add(WFSPath(item.flop, timesLocal2, lvNames[i]));
							});
						CocoaDialog.savePanel(
								{ |path| allMovs.asWFSPathArray.writeWFSFile( path ); },
								{ allMovs.asWFSPathArray.writeWFSFile( nil ).postln; }
							);
					  }
					  { popUp.value == 7 } // import SVG
					  {   CocoaDialog.getPaths(	
							{ arg paths;
								var svgFile, pathArray, useCurves = false;
								var doFuncSVG;
								svgFile = SVGFile.read( paths[0] );
								doFuncSVG = {
									pathArray = WFSPathArray.fromSVGFile( svgFile, useCurves: useCurves );
									SCAlert( "% paths found\nhow do you want to import?"
										.format( pathArray.size ), [ "cancel", "add", "replace all" ],
											[ { }, { pathArray.do({
												|item| addWFSPath.value(item); }) }, 
												{ WFSPathEditor( pathArray ); } ] );
								};
								
								if( svgFile.hasCurves )
									{ SCAlert( "file '%'\nincludes curved segments. How to import?"
												.format( svgFile.path.basename ),
											[ "cancel", "lines only", "curves" ],
											[ {},{ doFuncSVG.value },
											  { useCurves = true;  doFuncSVG.value } ]  
											); }
									{ doFuncSVG.value };
								
						});
					}
					  { popUp.value == 8 } // export SVG
					  {	allMovs = [];
						lvRealMulti.do({ |item, i|
							var timesLocal2;
							timesLocal2 = lvTimes[i];
							timesLocal2 = timesLocal2.collect({ |item| 
								var out;
								out = item - lastTime;
								lastTime = item;
								out;
								});
							timesLocal2 = timesLocal2[1..];  
							allMovs = allMovs.add(WFSPath(item.flop, timesLocal2, lvNames[i]));
							});
						CocoaDialog.savePanel(
								{ |path| allMovs.asWFSPathArray.writeSVGFile( path ); },
								{ allMovs.asWFSPathArray.writeSVGFile( nil ).postln; }
							);
					  }
					  { popUp.value == 9 } // export EPS
					  {	allMovs = [];
						lvRealMulti.do({ |item, i|
							var timesLocal2;
							timesLocal2 = lvTimes[i];
							timesLocal2 = timesLocal2.collect({ |item| 
								var out;
								out = item - lastTime;
								lastTime = item;
								out;
								});
							timesLocal2 = timesLocal2[1..];  
							allMovs = allMovs.add(WFSPath(item.flop, timesLocal2, lvNames[i]));
							});
						
						CocoaDialog.savePanel(
								{ |path| 
									File.checkDo( path.replaceExtension( "eps" ),
										SVGGroup( allMovs.asWFSPathArray.asSVGFile( path ).objects )
											.asPenFunction.asPostScript( 600@600 ) ); 
											}							);
					  }
					  ;
				popUp.value = 0;
				} );
			
				
		RoundButton(lw, Rect(375, 0, 50, 18)).radius_(0).border_(1).states_([["test"] ])
			.action_({ |button|
				WFSScore[ WFSEvent(0, WFSSynth( 'linear_blip', this.current ) ) ].edit; // open score editor
				});
					
		StaticText( lw,  Rect(290, 0, 50, 18) ).string_( "animate" );
			
		animate = [RoundButton.new(lw, Rect(335, 0, 18, 18)).radius_(0).border_(1).states_([
			[\play, Color.black, Color.gray(0.75)], 
			[\stop, Color.black, Color.gray(0.75)]])
			.canFocus_( false ),
					SmoothSlider.new(lw, Rect(355, 0, 10, 18)).value_(0.5).thumbSize_( 2 ),
					false]; //animate -- third slot: isAnimating
					
		animateFunc = { 
			var i = 0, newV, startTime, maxTime, speed;
			var envs;
			if(selected.name != 'all')
			{
				AppClock.sched(0.01,
					{ 	|time|
				startTime = startTime ? time;
				speed = animate.at(1).value.linexp(0.0, 1.0, 4.0, 0.25);	
				newV = WFSPath(ls.value.flop, name: 'temp').timeLine_(times * speed)
						.asEnvs[..1]
						.collect({ |env| env.at(time - startTime) });
				
				selectView.value = [[newV.at(0)],[newV.at(1)]];
				if(((time - startTime) > ((times * speed).last + 0.001)) or: animate[2].not)
					{nPoint.valueAction_(lvReal.at(0).size - 1);
					animate[0].value = 0;  animate[2] = false;
					nil}
					{0.01}
					}) 
				}
			{maxTime = lvTimes.collect({|item| item.last}).maxItem;
				AppClock.sched(0.01,
					{ 	|time|
				startTime = startTime ? time;
				speed = animate.at(1).value.linexp(0.0, 1.0, 4.0, 0.25);
				newV = Array.fill(lsMulti.size, { |iii|
						WFSPath(lsMulti.at(iii).value.flop, name: 'temp').timeLine_(lvTimes.at(iii) * speed)
							.asEnvs[..1]
							.collect({ |env| env.at(time - startTime) });
							
					});
				selectView.value = newV.flop;
				if(((time - startTime) > ((maxTime * speed) + 0.001)) or: animate[2].not)
					{ animate[0].value = 0;  animate[2] = false;
					nil}
					{0.01}
					})};
			};
			
		animate.at(0).action = {|button|	if(button.value == 1)  { 
				animate[2] = true;
				animateFunc.value;
				} {
				animate[2] = false;
				};
			 };
		
		
		lsMulti = makeMultiDisplay.value(lvRealMulti,lvNames);
		fitAll.value;
		
		// colors to the edit section
		/*
		SCCompositeView(lw,Rect(498, 0 , 84 ,102)).background_( Color.black.alpha_(0.125) ); // scale
		SCCompositeView(lw,Rect(498, 102 , 84 ,42)).background_( Color.yellow.alpha_(0.125) ); // rotate
		SCCompositeView(lw,Rect(498, 144 , 84 ,44)).background_( Color.black.alpha_(0.125) ); // flip/repeat
		SCCompositeView(lw,Rect(498, 188 , 84 ,42)).background_( Color.yellow.alpha_(0.125) ); // move
		SCCompositeView(lw,Rect(498, 230 , 84 ,22)).background_( Color.black.alpha_(0.125) ); // quantize
		SCCompositeView(lw,Rect(498, 252 , 84 ,62)).background_( Color.yellow.alpha_(0.125) );  // interpolate
		SCCompositeView(lw,Rect(498, 314 , 84 ,22)).background_( Color.black.alpha_(0.125) );  // interpolate
		*/
	
		
		//SCCompositeView(lw,Rect(0, 548 , 650 , 152)).background_( Color.gray.alpha_(0.3) );  // times
		
		

		
		
		lw.front;
		lw.refresh;
				
		addWFSPath = { arg aWFSPath;
						var name, lastChar, preventInfLoop = 0;
						historySwitch.addItem;
						// convert 
						name = aWFSPath.name.asString;
						// rename if needed (not fully functional yet)
						if( lvNames.any({ |item| item == name.asSymbol }) )
							{ lastChar = name.last;
							if( lastChar.isAlpha && ( lastChar != $z )  )
								{ 
								while { ( preventInfLoop < 100 ) and: ( lvNames.any({ |item| item == name.asSymbol }) ) }
									{ name = name[..name.size-2] ++ (lastChar.digit + 1).asDigit.toLower; 
										lastChar = name.last;
										preventInfLoop = preventInfLoop + 1; };
								}
								{ name = (name ++ "a"); 
									lastChar = $a;
								while { ( preventInfLoop < 100 ) and: ( lvNames.any({ |item| item == name.asSymbol }) ) }
									{ name = name[..name.size-2] ++ (lastChar.digit + 1).asDigit.toLower; 
										lastChar = name.last;
										preventInfLoop = preventInfLoop + 1; };
								
								};
							("changed name of added path to: " ++ name).postln;
							};
						lvNames = lvNames.add(name.asSymbol);
						lvRealMulti = lvRealMulti.add(aWFSPath.asXYArray.flop);
						times = aWFSPath.timeLine;
						lvTimes = lvTimes.add(times);
						
						//update
						selected.items_(lvNames.copy ++ ['-','all']);
						fillMorphTo.value;
						selected.valueAction_(lvNames.size - 1);
						colorAgain = true;
						fitAll.value;
					};
						
		
		refreshViews = { 
						historySwitch.addItem;
						selected.items_(lvNames.copy ++ ['-','all']);
						fillMorphTo.value;
						selected.valueAction_(lvNames.size - 1);
						colorAgain = true;
						fitAll.value;
						};
						
		flopFunc = {
				var lvRealCopy;
				lvRealCopy = lvRealMulti.collect({ |item| item.flop }).flop;
				lvRealMulti = lvRealCopy.collect({ |item| item.flop });
				lvNames = Array.fill(lvRealMulti.size, {|i| ("M" ++ (i+1).asString).asSymbol});
				historySwitch.addItem;
				removeMultiDisplay.(lsMulti);
				lsMulti = makeMultiDisplay.value(lvRealMulti,lvNames);
				lvTimes = Array.fill(lvRealMulti.size, {|ii|
					Array.fill(lvRealMulti.at(ii).at(0).size, { |i|
						//i / ((lvRealMulti.at(ii).at(0).size - 1).max(1)) ;
						i;});
					});
				selected.items_(lvNames.copy ++ ['-','all']);
				selected.valueAction_(lvNames.size - 1);
				
				fillMorphTo.value;
				colorAgain = true;
				fitAll.value;
					};
		
		window = lw;
		
		if( generateNewAtStartup ) { 
			generateWindow.value; 
			removeMovement.value(0); 
			};
		
		//^window;
		^WFSPathEditor; // returns a meta class
}
}


