package api

import io.vertx.core.json.JsonObject
import io.vertx.groovy.core.Vertx
import org.folio.inventory.InventoryVerticle
import org.folio.metadata.common.testing.HttpClient
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.CompletableFuture

class ModIngestExamples extends Specification {

  public static final testPortToUse = 9603

  private Vertx vertx;
  private final HttpClient client = new HttpClient()

  def setupSpec() {
    startVertx()
    startInventoryVerticle()
  }

  def cleanupSpec() {
    stopVertx()
  }

  void "Ingest some MODS records"() {
    given:
      def modsFile = loadFileFromResource(
        "mods/multiple-example-mods-records.xml")

    when:
      def (ingestResponse, body) = beginIngest([modsFile])

    then:
      def statusLocation = ingestResponse.headers.location.toString()
      assert ingestResponse.status == 202
      assert statusLocation != null

      def conditions = new PollingConditions(
        timeout: 10, initialDelay: 1.0, factor: 1.25)

      conditions.eventually {
        ingestJobHasCompleted(statusLocation)
        expectedItemsCreatedFromIngest()
      }
  }

  void "Refuse ingest for multiple files"() {
    given:
      def modsFile = loadFileFromResource("mods/multiple-example-mods-records.xml")

    when:
      def (resp, body) = beginIngest([modsFile, modsFile])

    then:
      assert resp.status == 400
      assert body == "Cannot parsing multiple files in a single request"
  }

  private ingestJobHasCompleted(String statusLocation) {
    def client = new HttpClient()

    def (resp, body) = client.get(statusLocation)

    assert resp.status == 200
    assert body.status == "completed"
  }

  private expectedItemsCreatedFromIngest() {
    def client = new HttpClient()

    def (resp, body) = client.get(new URL("http://localhost:9603/inventory/items"))

    assert resp.status == 200
    assert body.items != null

    List<JsonObject> items = body

    assert items.size() == 5
    assert items.every({ it.id != null })
    assert items.every({ it.title != null })
    assert items.every({ it.barcode != null })

    assert items.any({
      matches(it,
        "California: its gold and its inhabitants, by the author of 'Seven years on the Slave coast of Africa'.",
        "69228882")
    })

    assert items.any({
      matches(it,
        "Studien zur Geschichte der Notenschrift.",
        "69247446")
    })

    assert items.any({
      matches(it,
        "Essays on C.S. Lewis and George MacDonald",
        "53556908")
    })

    assert items.any({
      matches(it,
        "Statistical sketches of Upper Canada, for the use of emigrants, by a backwoodsman [W. Dunlop].",
        "69077747")
    })

    assert items.any({
      matches(it,
        "Edward McGuire, RHA",
        "22169083")
    })
  }


  private startVertx() {
    vertx = Vertx.vertx()
  }

  private startInventoryVerticle() {
    InventoryVerticle.deploy(vertx, ["port": testPortToUse]).join()
  }

  private stopVertx() {
    if (vertx != null) {
      def stopped = new CompletableFuture()

      vertx.close({ res ->
        if (res.succeeded()) {
          stopped.complete(null);
        } else {
          stopped.completeExceptionally(res.cause());
        }
      })

      stopped.join()
    }
  }

  private beginIngest(List<File> files) {
    client.uploadFile(new URL("http://localhost:9603/inventory/ingest/mods"),
      files, "record")
  }

  private File loadFileFromResource(String filename) {
    ClassLoader classLoader = getClass().getClassLoader();

    new File(classLoader.getResource(filename).getFile())
  }

  private boolean matches(record, String expectedTitle, String expectedBarcode) {
    record.title == expectedTitle &&
      record.barcode == expectedBarcode
  }

}
