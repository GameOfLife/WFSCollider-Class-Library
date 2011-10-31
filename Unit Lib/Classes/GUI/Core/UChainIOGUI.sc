/*
    Unit Library
    The Game Of Life Foundation. http://gameoflife.nl
    Copyright 2006-2011 Miguel Negrao, Wouter Snoei.

    GameOfLife Unit Library: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GameOfLife Unit Library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GameOfLife Unit Library.  If not, see <http://www.gnu.org/licenses/>.
*/

UChainIOGUI : UChainGUI {
	
	var <analyzers;
	var <unitColors;
	var <>audioMax = 0, <>controlMax = 0;
	
	makeViews { |bounds|
		
		analyzers = ( 
			\audio: UChainAudioAnalyzer(chain),
			\control:  UChainControlAnalyzer(chain)
		);
		
		RoundView.useWithSkin( skin ++ (RoundView.skin ? ()), {
			this.prMakeViews( bounds );
		});
		
	}
	
	getHeight { |units, margin, gap|
		^units.collect({ |unit, i|
			((14 + gap.y) * (
				analyzers[ \audio ].numIOFor( i ) + 
				analyzers[ \control ].numIOFor( i ))
			) +
				14 + gap.y + gap.y;
		}).sum + (4 * (14 + gap.y));
	}
	
	makeUnitHeader { |units, margin, gap|
		var comp, header,params;
		
		comp = CompositeView( composite, (composite.bounds.width - (margin.x * 2))@16 )
			.resize_(2);
		
		header = StaticText( comp, comp.bounds.moveTo(0,0) )
				.applySkin( RoundView.skin )
				.string_( " units" )
				.align_( \left )
				.resize_(2);
		
				
		params = SmoothButton( comp, Rect( comp.bounds.right - (60+2), 1, 60, 12 ) )
			.label_( "params" )
			.border_( 1 )
			.radius_( 2 )
			.action_({
				var parent;
				parent = composite.parent;
				{
					composite.remove;
					UChainGUI( parent, originalBounds, chain );
				}.defer(0.01);
			}).resize_(3);
						
		CompositeView( comp, Rect( 0, 14, (composite.bounds.width - (margin.x * 2)), 2 ) )
			.background_( Color.black.alpha_(0.25) )
			.resize_(2)

	}
	
	makeUnitSubViews { |scrollView, units, margin, gap|
		var unitInitFunc;
		var labelWidth;
		var width;
		
		width = scrollView.bounds.width - 12 - (margin.x * 2);
		
		labelWidth = 80;
		
		if( RoundView.skin.notNil ) { labelWidth = RoundView.skin.labelWidth ? 80 };

		unitInitFunc = { |unit, what ...args|
			if( what === \init ) { // close all views and create new
				chain.changed( \units );
			};
		};
		
		unitColors = units.collect({ |item, i|
			Color.hsv( i.linlin( 0, units.size, 0, 1 ), 0.1, 0.9 );
		});
		
		^units.collect({ |unit, i|
			var header, comp, views, params;
			
			comp = CompositeView( scrollView, width@14 )
				.resize_(2);
			
			header = StaticText( comp, comp.bounds.moveTo(0,0) )
				.applySkin( RoundView.skin )
				.string_( " " ++ i ++ ": " ++ unit.defName )
				.background_( unitColors[i] )
				.resize_(2)
				.font_( 
					(RoundView.skin.tryPerform( \at, \font ) ?? 
						{ Font( Font.defaultSansFace, 12) }).boldVariant 
				);
			
			views = this.makeUnitView( scrollView, unit, i, labelWidth, width );
			
			unit.addDependant( unitInitFunc );
			
			header.onClose_({ 
				unit.removeDependant( unitInitFunc );
				views[ \ctrl ].remove;
			});
			
			views;
			
		});
	
	}
	
	getMax { |rate = \audio|
		^analyzers[ rate ].usedBuses.maxItem ? 0;
	}
	
	makeUnitView { |scrollView, unit, i, labelWidth, width|
		var ctrl;
		var setPopUps;
		var max;
		var views;
		
		max = (
			\audio: this.getMax( \audio ),
			\control: this.getMax( \control )
		);
		
		views = ();
		
		ctrl = SimpleController( unit );
		
		[ 
			[ \audio, \in ], 
			[ \audio, \out ], 
			[ \control, \in ], 
			[ \control, \out ] 
		].do({ |item|
			var rate, mode;
			var io, etter, setter, getter;
			#rate, mode = item;
			etter = "et" ++ (rate.asString.firstToUpper ++ mode.asString.firstToUpper);
			setter = ("s" ++ etter).asSymbol;
			getter = ("g" ++ etter).asSymbol;
			
			io = analyzers[ rate ].ioFor( mode, i );
			
			if( io.notNil ) {
				
				views[ rate ] = views[ rate ] ? ();
				views[ rate ][ mode ] = [ ];
				
				io[2].do({ |item, ii|
					var nb, pu;
					
					StaticText( scrollView, labelWidth @ 14 )
						.applySkin( RoundView.skin )
						.align_( \right )
						.string_( "% %".format( 
							if( ii == 0 ) { "% %".format( rate, mode ) } { "" }, item ) 
						);
						
					nb = SmoothNumberBox( scrollView, 20@14 )
						.clipLo_( 0 )
						.value_(  io[3][ii] )
						.action_( { |nb|
							unit.perform( setter, ii, nb.value ); 					} );
						
					pu = PopUpMenu( scrollView, (width - labelWidth - 28)@14 )
						.applySkin( RoundView.skin )
						.resize_(2)
						.action_({ |pu| 
							unit.perform( setter, ii, pu.value );
						});
					
					this.setPopUp( pu, io[3][ii], i, rate, mode, max[ rate ] );
					
					setPopUps = setPopUps.addFunc({
						this.setPopUp( pu, nb.value, i, rate, mode, max[ rate ] );
					});
					
					views[ rate ][ mode ] = views[ rate ][ mode ].add( [nb, pu] );
					
					ctrl.put( unit.getIOKey( mode, rate, ii ), { |obj, what, val|
						nb.value = val;
						analyzers[ rate ].init;
						max[ rate ] = this.getMax( rate );
						uguis.do({ |vws| vws[ \setPopUps ].value; });
					});
						
					composite.decorator.nextLine;
				});		
			};
		});
		
		if( views.size == 0 ) { 
			ctrl.remove;
		} {
			views[ \ctrl ] = ctrl;
		};
		views[ \setPopUps ] = setPopUps;
		^views;
		
	}
	
	setPopUp { |pu, value = 0, i = 0, rate = \audio, mode = \in, max = 10|
		var busConnections;
		busConnections = this.getBusConnections( i, rate, mode, max );
		{ 	
			pu.items = this.prGetPopUpItems( busConnections, mode ); 
			pu.value = value.clip( 0, busConnections.size-1);
			pu.background = this.getPopUpColor( busConnections, value );
		}.defer;
	}
	
	getBusConnections { |i = 0, rate = \audio, mode = \in, max = 10|
		var items, lastNotNil = 0;
		var func;
		
		func = { |bus| analyzers[ rate ].busConnection( mode, bus.asInt, i.asInt ) };
				
		items = (max+2).asInt.collect( func );
		items.do({ |item, ii|
			if( item.notNil ) {
				lastNotNil = ii;
			};
		});
		^items[..lastNotNil + 1];
	}
	
	prGetPopUpItems { |busConnections, mode = \in|
		
		var prefix;
		
		prefix = switch( mode, \in, "from", \out, "to" );
		
		^busConnections.collect({ |item, bus|
			if( item.notNil ) {
				"% %:% (%)"
					.format( 
						prefix,
						item[1], 
						item[0].defName,
						item[2][ 
							item[3].indexOfEqual( bus.asInt ) 
						]
					);
			} {
				"no signal"
			};
		});
					
	}

	getPopUpItems { |i = 0, rate = \audio, mode = \in, max = 10|
		^this.prGetPopUpItems( this.getBusConnections( i, rate, mode, max ), mode );	}
	
	getPopUpColor { |busConnections, bus = 0|
		var item;
		item = busConnections.clipAt( bus.asInt );
		if( item.notNil ) {
			^unitColors[ item[1] ]
		} {
			^Color.clear;
		};
	}

}