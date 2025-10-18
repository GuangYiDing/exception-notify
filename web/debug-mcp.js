// Debug script to test MCP tool integration
// This can be run in the browser console to test the flow

async function debugMcpIntegration() {
  console.log('🔍 Starting MCP integration debug...');

  // Check if we're on the right page
  if (!window.location.pathname.includes('/')) {
    console.error('❌ Please run this script on the main page');
    return;
  }

  // Get current settings
  const settings = JSON.parse(localStorage.getItem('exception-notify-ai-settings') || '{}');
  const mcpSettings = JSON.parse(localStorage.getItem('exception-notify-mcp-settings') || '{}');

  console.log('📋 Current AI Settings:', settings);
  console.log('🔧 Current MCP Settings:', mcpSettings);

  // Check GLM model
  const isGlmModel = settings.model?.toLowerCase().startsWith('glm');
  console.log(`🤖 Is GLM model: ${isGlmModel} (model: ${settings.model})`);

  // Check MCP server
  const activeServer = mcpSettings.servers?.find(server => server.isActive);
  console.log('🖥️ Active MCP Server:', activeServer);

  if (isGlmModel && activeServer) {
    console.log('✅ GLM + MCP configuration detected');

    try {
      // Test MCP connection
      console.log('🔗 Testing MCP connection...');
      const mcpResponse = await fetch('/api/mcp/tools/list', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          servers: [
            {
              baseUrl: activeServer.baseUrl,
              headers: activeServer.headersText ? JSON.parse(activeServer.headersText) : {},
              name: activeServer.name
            }
          ]
        })
      });

      if (mcpResponse.ok) {
        const mcpResult = await mcpResponse.json();
        console.log('✅ MCP tools list:', mcpResult);

        const serverDataList = mcpResult.data?.servers || [];
        const serverData =
          serverDataList.find((item) => item?.name && item.name === activeServer.name) || serverDataList[0];

        if (serverData?.error) {
          console.log('❗ MCP server error:', serverData.error);
        } else if (mcpResult.code === 0 && serverData?.tools?.length > 0) {
          console.log('🛠️ Available tools:', serverData.tools.map(t => t.name));
        } else {
          console.log('⚠️ No tools available');
        }
      } else {
        console.error('❌ MCP connection failed:', mcpResponse.status, await mcpResponse.text());
      }
    } catch (error) {
      console.error('❌ MCP test error:', error);
    }
  } else {
    console.log('ℹ️ No GLM + MCP configuration found');
    console.log('   - GLM Model:', isGlmModel);
    console.log('   - Active MCP Server:', !!activeServer);
  }
}

// Function to simulate a GLM tool call response
function simulateGlmToolCall() {
  console.log('🎭 Simulating GLM tool call response...');

  const mockToolCall = {
    id: 'call_123456',
    type: 'mcp',
    mcp: {
      name: 'test_tool',
      arguments: { query: 'test' }
    }
  };

  console.log('🔧 Mock tool call:', mockToolCall);

  // This simulates what would happen in the handleToolCalls function
  console.log('📝 This would trigger tool execution via MCP API');
}

// Auto-run debug
debugMcpIntegration();

// Make functions available globally for manual testing
window.debugMcpIntegration = debugMcpIntegration;
window.simulateGlmToolCall = simulateGlmToolCall;

console.log('🎯 Debug functions available:');
console.log('   - debugMcpIntegration()');
console.log('   - simulateGlmToolCall()');
