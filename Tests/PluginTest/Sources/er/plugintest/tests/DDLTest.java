package er.plugintest.tests;

import java.sql.*;

import com.webobjects.eoaccess.*;
import com.webobjects.foundation.*;

import er.extensions.eof.*;
import er.extensions.jdbc.*;

public class DDLTest extends PluginTest {

	public DDLTest(String name) {
		super(name);
	}

	public void testDB2CreateSchema() {
		//		ERXTestUtilities.fixModelsForAdaptorNamed(adaptorName);


		ERXEC ec = (ERXEC) ERXEC.newEditingContext();
		ec.lock();
		try {
			ERXEOAccessUtilities.ChannelAction action = new ERXEOAccessUtilities.ChannelAction() {
				@Override
				protected int doPerform(EOAdaptorChannel channel) {
					try {
						ERXSQLHelper helper = ERXSQLHelper.newSQLHelper(adaptorName);
						String sql = helper.createSchemaSQLForEntitiesInModelAndOptions(model.entities(), model, helper.defaultOptionDictionary(false, true)) ;
						log.debug(sql);
						try {
							ERXJDBCUtilities.executeUpdateScript(channel, sql);
						} catch (Throwable e) {
							log.info("drop failure");
						}
						sql = helper.createSchemaSQLForEntitiesInModelAndOptions(model.entities(), model, helper.defaultOptionDictionary(true, false)) ;
						log.debug(sql);
						ERXJDBCUtilities.executeUpdateScript(channel, sql);
					}
					catch (SQLException e) {
						throw new NSForwardException(e);
					}
					return 0;
				}
			};
			action.perform(ec, model.name());
		} finally {
			ec.unlock();
		}
	}
	
}
