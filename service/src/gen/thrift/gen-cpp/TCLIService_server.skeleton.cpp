// This autogenerated skeleton file illustrates how to build a server.
// You should copy it to another filename to avoid overwriting it.

#include "TCLIService.h"
#include <thrift/protocol/TBinaryProtocol.h>
#include <thrift/server/TSimpleServer.h>
#include <thrift/transport/TServerSocket.h>
#include <thrift/transport/TBufferTransports.h>

using namespace ::apache::thrift;
using namespace ::apache::thrift::protocol;
using namespace ::apache::thrift::transport;
using namespace ::apache::thrift::server;

using boost::shared_ptr;

using namespace  ::apache::hive::service::cli::thrift;

class TCLIServiceHandler : virtual public TCLIServiceIf {
 public:
  TCLIServiceHandler() {
    // Your initialization goes here
  }

  void OpenSession(TOpenSessionResp& _return, const TOpenSessionReq& req) {
    // Your implementation goes here
    printf("OpenSession\n");
  }

  void CloseSession(TCloseSessionResp& _return, const TCloseSessionReq& req) {
    // Your implementation goes here
    printf("CloseSession\n");
  }

  void GetInfo(TGetInfoResp& _return, const TGetInfoReq& req) {
    // Your implementation goes here
    printf("GetInfo\n");
  }

  void ExecuteStatement(TExecuteStatementResp& _return, const TExecuteStatementReq& req) {
    // Your implementation goes here
    printf("ExecuteStatement\n");
  }

  void GetTypeInfo(TGetTypeInfoResp& _return, const TGetTypeInfoReq& req) {
    // Your implementation goes here
    printf("GetTypeInfo\n");
  }

  void GetCatalogs(TGetCatalogsResp& _return, const TGetCatalogsReq& req) {
    // Your implementation goes here
    printf("GetCatalogs\n");
  }

  void GetSchemas(TGetSchemasResp& _return, const TGetSchemasReq& req) {
    // Your implementation goes here
    printf("GetSchemas\n");
  }

  void GetTables(TGetTablesResp& _return, const TGetTablesReq& req) {
    // Your implementation goes here
    printf("GetTables\n");
  }

  void GetTableTypes(TGetTableTypesResp& _return, const TGetTableTypesReq& req) {
    // Your implementation goes here
    printf("GetTableTypes\n");
  }

  void GetColumns(TGetColumnsResp& _return, const TGetColumnsReq& req) {
    // Your implementation goes here
    printf("GetColumns\n");
  }

  void GetFunctions(TGetFunctionsResp& _return, const TGetFunctionsReq& req) {
    // Your implementation goes here
    printf("GetFunctions\n");
  }

  void GetOperationStatus(TGetOperationStatusResp& _return, const TGetOperationStatusReq& req) {
    // Your implementation goes here
    printf("GetOperationStatus\n");
  }

  void CancelOperation(TCancelOperationResp& _return, const TCancelOperationReq& req) {
    // Your implementation goes here
    printf("CancelOperation\n");
  }

  void CloseOperation(TCloseOperationResp& _return, const TCloseOperationReq& req) {
    // Your implementation goes here
    printf("CloseOperation\n");
  }

  void GetResultSetMetadata(TGetResultSetMetadataResp& _return, const TGetResultSetMetadataReq& req) {
    // Your implementation goes here
    printf("GetResultSetMetadata\n");
  }

  void FetchResults(TFetchResultsResp& _return, const TFetchResultsReq& req) {
    // Your implementation goes here
    printf("FetchResults\n");
  }

  void GetQueryPlan(TGetQueryPlanResp& _return, const TGetQueryPlanReq& req) {
    // Your implementation goes here
    printf("GetQueryPlan\n");
  }

  void GetDelegationToken(TGetDelegationTokenResp& _return, const TGetDelegationTokenReq& req) {
    // Your implementation goes here
    printf("GetDelegationToken\n");
  }

  void CancelDelegationToken(TCancelDelegationTokenResp& _return, const TCancelDelegationTokenReq& req) {
    // Your implementation goes here
    printf("CancelDelegationToken\n");
  }

  void RenewDelegationToken(TRenewDelegationTokenResp& _return, const TRenewDelegationTokenReq& req) {
    // Your implementation goes here
    printf("RenewDelegationToken\n");
  }

};

int main(int argc, char **argv) {
  int port = 9090;
  shared_ptr<TCLIServiceHandler> handler(new TCLIServiceHandler());
  shared_ptr<TProcessor> processor(new TCLIServiceProcessor(handler));
  shared_ptr<TServerTransport> serverTransport(new TServerSocket(port));
  shared_ptr<TTransportFactory> transportFactory(new TBufferedTransportFactory());
  shared_ptr<TProtocolFactory> protocolFactory(new TBinaryProtocolFactory());

  TSimpleServer server(processor, serverTransport, transportFactory, protocolFactory);
  server.serve();
  return 0;
}

