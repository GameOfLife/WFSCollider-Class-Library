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

UdefListView {
	
	var view, views;
	
	*new { |parent, bounds|
		^super.new.init( parent, bounds );
	}
	
	init { |parent, bounds|
		
		var categories, names;
		
		bounds = bounds ? 200@400;
		
		view = EZCompositeView( parent, bounds ).resize_(5);
		bounds = view.bounds;
		
		views = ();
		
		views[ \scrollview ] = ScrollView( view, view.bounds.moveTo(0,0) ).resize_(5);
		views[ \scrollview ].addFlowLayout;
		
		categories = [];
		
		Udef.all.keys.asArray.sort.do({ |key|
			var category, index, udef;
			udef = Udef.all[ key ];
			category = udef.category;
			index = categories.indexOf( category );
			if( index.isNil ) {
				categories = categories ++ [ category, [ udef ] ];
			} {
				categories[ index + 1 ] = categories[ index + 1 ].add( udef );
			};
		});
		
		RoundView.useWithSkin( UChainGUI.skin ++ (RoundView.skin ? ()), {
			categories.pairsDo({ |cat, udefs|
				views[ cat ] = ExpandView( views[ \scrollview ], 
					(bounds.width - 18)@( (udefs.size + 1) * 22 ),
					(bounds.width - 18)@18,
					false 
				);
				
				views[ cat ].addFlowLayout( 0@0, 4@4 );
				
				StaticText( views[ cat ], (bounds.width - 36)@18 )
					.string_( " " ++ cat.asString )
					.applySkin( RoundView.skin ? () );
					
				udefs.do({ |udef|
					DragSource( views[ cat ], (bounds.width - 36)@18 )
						.object_( udef )
						.string_( " " ++ udef.name.asString )
						.applySkin( RoundView.skin ? () );
				});
			});
		});
	}
	
}