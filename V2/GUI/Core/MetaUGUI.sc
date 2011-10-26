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

MetaUGUI {
	
	var <metaUnit;
	
	var <parent, <composite, <views, <controller;
	var <viewHeight = 16, <labelWidth = 50;
	var <>action;
	
	*new { |parent, bounds, metaUnit|
		^super.newCopyArgs( metaUnit ).init( parent, bounds );
	}
	
	init { |inParent, bounds|
		parent = inParent;
		if( parent.isNil ) { parent = Window( metaUnit.defName ).front };
		this.makeViews( bounds );
	}
	
	makeViews { |bounds|
		var margin = 0@0, gap = 4@4;
		
		bounds = bounds ?? { parent.asView.bounds.insetBy(4,4) };
		bounds.height = (margin.y * 2) + 
			( metaUnit.argNames.size * (viewHeight + gap.y) ) - gap.y;
		
		controller = SimpleController( metaUnit );
		
		composite = CompositeView( parent, bounds ).resize_(2);
		composite.addFlowLayout( margin, gap );
		composite.onClose = { controller.remove };
		
		views = ();
		StaticText(composite,100@20).string_("Meta Args:");
		composite.decorator.nextLine;
		metaUnit.args.pairsDo({ |key, value, i|
			var vw, argSpec;
			
			argSpec = metaUnit.def.argSpecs[i/2];
			
			if( argSpec.private.not ) { // show only if not private
				vw = ObjectView( composite, nil, metaUnit, key, 
						argSpec.spec, controller );
				
				vw.action = { action.value( this, key, value ); };
				
				views[ key ] = vw;
			}
		
		});
		StaticText(composite,100@20).string_("Args:");
		composite.decorator.nextLine;
		metaUnit.unit.gui(composite)

	}
	
	resize_ { |resize| composite.resize_(resize) }
	reset { metaUnit.reset }
	
	font_ { |font| views.values.do({ |vw| vw.font = font }); }
	viewHeight_ { |height = 16|
		views.values.do({ |vw| vw.view.bounds = vw.view.bounds.height_( height ) });
		composite.decorator.reFlow( composite );
	}
	labelWidth_ { |width=50|
		labelWidth = width;
		views.values.do(_.labelWidth_(width));
	}
	
	view { ^composite }
}

+ MetaU {
	gui { |parent, bounds| ^MetaUGUI( parent, bounds, this ) }
}