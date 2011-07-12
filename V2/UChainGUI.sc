// a styled gui in user-defined window
// -- to be replaced by WFSChainGUI later --
//temporary
UChainGUI {

    *new{ |x|
        var y,w = Window( "x", Rect( 300,25,200,300 ) ).front;
        w.addFlowLayout;
        RoundView.useWithSkin( (
            labelWidth: 40,
            font: Font( Font.defaultSansFace, 10 ),
            hiliteColor: Color.gray(0.33)
        ), {
            SmoothButton( w, 16@16 )
                .label_( ['power', 'power'] )
                .hiliteColor_( Color.green.alpha_(0.5) )
                .action_( [ { x.start }, { x.stop } ] )
                .value_( (x.groups.size > 0).binaryValue );
            w.view.decorator.nextLine;
            y = x.units.collect({ |item|
                SmoothButton( w, 16@16 )
                .label_( ['power', 'power'] )
                .hiliteColor_( Color.green.alpha_(0.5) )
                .action_( [ { item.paused_(false) }, { item.paused_(true) } ] )
                .value_( 1 );

                StaticText( w, (w.view.bounds.width - 60)@16 )
                    .string_( " " ++ item.defName.asString )
                    .font_( RoundView.skin.font.boldVariant )
                    .background_( Color.gray(0.8) );
                item.gui( w );
            });
        });
    }
}
/*

w = Window( "x", Rect( 300,25,200,300 ) ).front;
w.addFlowLayout;
RoundView.useWithSkin( (
	labelWidth: 40,
	font: Font( Font.defaultSansFace, 10 ),
	hiliteColor: Color.gray(0.33)
), {
	SmoothButton( w, 16@16 )
		.label_( ['power', 'power'] )
		.hiliteColor_( Color.green.alpha_(0.5) )
		.action_( [ { x.start }, { x.stop } ] )
		.value_( (x.groups.size > 0).binaryValue );
	y = x.units.collect({ |item|
		StaticText( w, (w.view.bounds.width - 8)@16 )
			.string_( " " ++ item.defName.asString )
			.font_( RoundView.skin.font.boldVariant )
			.background_( Color.gray(0.8) );
		item.gui( w );
	});
});
*/