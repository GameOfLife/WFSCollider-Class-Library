SyncCenterGui {
	classvar <window, <controllers;
	
	*new{
		var font = Font( Font.defaultSansFace, 11 ), masterText, masterUV;		
		if( window.notNil ){
			if( window.isClosed.not) {
				controllers.do(_.remove);
				window.close
			};
			window = nil;
		};
		window = Window("Sync Center",Rect(300,300,400,400)).front;
		window.addFlowLayout;
		
		SmoothButton( window, 100@30  )
 			.states_( [[ "Sync", Color.black, Color.clear ]] )
 			.canFocus_(false)
			.radius_( 0 )
			.border_(1)
			.font_( Font( font.name, 10 ).boldVariant )
			.action_({ 
				SyncCenter.remoteSync			
			});
			
		window.view.decorator.nextLine;
		
		masterText = StaticText(window,100@20).string_("Master:");
		masterUV = UserView(window,20@20).background_(Color.red);
		controllers = List.new;
		controllers.add(Updater(SyncCenter.masterCount, { |mCount|
			{
				masterText.string_("Master: "++mCount.value);
				masterUV.background_(Color.green)
			}.defer
		}));
		window.view.decorator.nextLine;
		SyncCenter.servers.do{ |server,i|
			var text, uv;
			text = StaticText(window,100@20).string_(server.name++":" );
			uv = UserView(window,20@20).background_(Color.red);
			controllers.add(Updater(SyncCenter.remoteCounts[i], { |count|
				{
					text.string_(server.name++": "+count.value);
					uv.background_(Color.green)
				}.defer
			}));
			window.view.decorator.nextLine;
		};
		
		window.onClose_({ this.remove });
			
	}
	
	remove{
		controllers.do(_.remove);
	}
}
