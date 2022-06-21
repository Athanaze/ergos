/*
 * scala-swing (https://www.scala-lang.org)
 *
 * Copyright EPFL, Lightbend, Inc., contributors
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 *
 * See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 */

import Ergos.{getBestFullHeaderDate, getDifferenceDays, getNewCommandOutputSeparator, linkBuilder, nColumns}
import requests.Response

import javax.swing.UIManager
import ujson._

import java.awt.{Color, Cursor, Desktop}
import java.io.IOException
import java.net.URISyntaxException
import java.util.Date
import scala.swing.ListView._
import scala.swing.Swing._
import scala.swing._
import scala.swing.event._
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.concurrent.TimeUnit
import scala.swing.MenuBar.NoMenuBar
import java.net.URI
import scala.collection.mutable
import scala.util.control.Exception
import sys.process._
object Ergos {
  val nColumns = 40

  def main(args: Array[String]) = {
    Storage.addWallet("wallet_2", BIP39.genMnemonic())
    Storage.getListWallets().foreach(liValue => {
      println(liValue.name)
    })

  }

  def getNewCommandOutputSeparator(): String = {
    import java.time.Instant
    import java.time.ZoneId
    import java.time.format.DateTimeFormatter
    val formatter = DateTimeFormatter.ofPattern( " hh:mm:ss ").withZone(ZoneId.systemDefault)

    val formattedInstant = formatter.format(Instant.now())

    val n =  nColumns - formattedInstant.length

    if (n > 1){
      val firstHalf = n/2
      ("-" * firstHalf)+formattedInstant+("-"*(nColumns-firstHalf-formattedInstant.length))+"\n"

    }else{
      formattedInstant+"\n"
    }
  }

  def getDifferenceDays(d: Date): Long = {
    TimeUnit.DAYS.convert(new Date().getTime - d.getTime, TimeUnit.MILLISECONDS)
  }



  def getBestFullHeaderDate(): Date = {
    try {
      val r = requests.get("""http://localhost:9053/info""")
      val responseText = r.text.replaceFirst(""" "fullBlocksScore".+,""", "").replaceFirst(""" "headersScore".+,""", "")

      val bestFullHeaderId = ujson.read(responseText)("bestFullHeaderId").str

      val data = ujson.read(requests.get("""https://api.ergoplatform.com/api/v1/blocks/"""+bestFullHeaderId).text)
      val timestamp = data("block")("blockTransactions")(0)("timestamp").value.toString.toDouble.toLong
      new Date(timestamp)
    } catch {
      case e: Exception => new Date()
    }
  }

  def linkBuilder(defaultText: String, url: String): Label = {
    new Label(defaultText){
      foreground = Color.BLUE.darker()
      cursor = new Cursor(Cursor.HAND_CURSOR)
      listenTo(mouse.clicks, mouse.moves)
      reactions += {
        case e: MousePressed => {
          try Desktop.getDesktop.browse(new URI(url))
          catch {
            case e1@(_: IOException | _: URISyntaxException) =>
              e1.printStackTrace()
          }
        }
        case e: MouseExited => {
          text = defaultText
        }
        case e: MouseEntered => {
          text = "<html><a href=''>" + defaultText + "</a></html>"
        }
        case _ => null // println ("Unreacted event")
      }
    }
  }

}

object UIDemo extends SimpleSwingApplication {
  try UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel")
  catch {
    case e: Exception =>
      System.out.println("ds")
  }
  val formatter = new SimpleDateFormat("dd/MM/yyyy")
  val bestFullHeaderDate = getBestFullHeaderDate()
  val terminalOuput = new TextArea(65, Ergos.nColumns){
    editable = false
    charWrap = true
    background = Color.BLACK
    foreground = Color.WHITE
  }
  val terminalScroll = new ScrollPane {
    contents = terminalOuput
  }

  def createWalletUI(ergosWallet: ErgosWallet): BoxPanel = {
    new BoxPanel(Orientation.Vertical) {
      border = CompoundBorder(TitledBorder(EtchedBorder, ergosWallet.name), EmptyBorder(5, 5, 5, 10))
      contents += new Label(ergosWallet.mnemonic)
      val copyMnemonicBtn =  new Button(){
        icon = Icon("copy.png")
      }
      listenTo(copyMnemonicBtn)
      reactions += {
        case ButtonClicked(`copyMnemonicBtn`) =>
          println(ergosWallet.mnemonic)
      }
      contents += copyMnemonicBtn
    }
  }

  def top: Frame = new MainFrame {
    title = "Ergos"

    /*
     * Create a menu bar with a couple of menus and menu items and
     * set the result as this frame's menu bar.
     */
    menuBar = new MenuBar {
      contents += new Menu("A Menu") {
        contents += new MenuItem("An item")
        contents += new MenuItem(Action("An action item") {
          println("Action '" + title + "' invoked")
        })

        contents += new Separator
        contents += new CheckMenuItem("Check me")
        contents += new CheckMenuItem("Me too!")
        contents += new Separator
        val a = new RadioMenuItem("a")
        val b = new RadioMenuItem("b")
        val c = new RadioMenuItem("c")
        val mutex = new ButtonGroup(a, b, c)
        contents ++= mutex.buttons
      }
      contents += new Menu("Empty Menu")
    }

    /*
     * The root component in this frame is a panel with a border layout.
     */
    contents = new BorderPanel {

      import BorderPanel.Position._

      var reactLive = false

      val tabs: TabbedPane = new TabbedPane {

        import TabbedPane._

        val buttons: FlowPanel = new FlowPanel {
          border = Swing.EmptyBorder(5, 5, 5, 5)
          contents += new BoxPanel(Orientation.Vertical){
            border = CompoundBorder(TitledBorder(EtchedBorder, "Local node"), EmptyBorder(5, 5, 5, 10))
            contents += linkBuilder("Local node panel", "http://localhost:9053/panel")
            contents += linkBuilder("Swagger API", "http://localhost:9053/swagger")
          }
          contents += new BoxPanel(Orientation.Vertical) {
            border = CompoundBorder(TitledBorder(EtchedBorder, "Radio Buttons"), EmptyBorder(5, 5, 5, 10))
            val a = new RadioButton("Green Vegetables")
            val b = new RadioButton("Red Meat")
            val c = new RadioButton("White Tofu")
            val mutex = new ButtonGroup(a, b, c)
            contents ++= mutex.buttons
          }
          contents += new BoxPanel(Orientation.Vertical) {
            border = CompoundBorder(TitledBorder(EtchedBorder, "Install development tools"), EmptyBorder(5, 5, 5, 10))
            val paintLabels = new CheckBox("IDEA Community Edition")
            paintLabels.selected = true
            val paintTicks = new CheckBox("VS Codium")
            paintTicks.selected = true
            val live = new CheckBox("Live")
            contents ++= Seq(paintLabels, paintTicks, live)
            val installDevToolsBtn = new Button("INSTALL")
            contents += installDevToolsBtn
            listenTo(paintLabels, paintTicks, live, installDevToolsBtn)
            reactions += {
              case ButtonClicked(`paintLabels`) =>
                slider.paintLabels = paintLabels.selected
              case ButtonClicked(`paintTicks`) =>
                slider.paintTicks = paintTicks.selected

              case ButtonClicked(`installDevToolsBtn`) => {
                val result = "ls -al".!!
                terminalOuput.text += getNewCommandOutputSeparator()
                terminalOuput.text += result
              }
              case ButtonClicked(`live`) =>
                reactLive = live.selected
            }

            listenTo(installDevToolsBtn)
          }

          contents += new ListView[BoxPanel]{

            Storage.getListWallets().foreach(wallet => {
              contents += createWalletUI(wallet)
            })
          }

          contents += terminalScroll
          contents += new BoxPanel(Orientation.Vertical){
            border = CompoundBorder(TitledBorder(EtchedBorder, "Syncing status"), EmptyBorder(5, 5, 5, 10))
            contents += new Label("Latest full header data : "+formatter.format(bestFullHeaderDate))

            contents += {
              val d = getDifferenceDays(bestFullHeaderDate)
              if(d > 0){new Label(d.toString+ " days behind")}else{null}

            }
          }


          contents += new Button(Action("Center Frame") {
            centerOnScreen()
          })
        }
        pages += new Page("Buttons"     , buttons)
        pages += new Page("GridBag"     , GridBagDemo       .ui)
        pages += new Page("Converter"   , CelsiusConverter2 .ui)
        pages += new Page("Tables"      , TableSelection    .ui)
        pages += new Page("Dialogs"     , Dialogs           .ui)
        pages += new Page("Combo Boxes" , ComboBoxes        .ui)
        pages += new Page("Split Panes" ,
          new SplitPane(Orientation.Vertical, new Button("Hello"), new Button("World")) {
            continuousLayout = true
          })

        val password: FlowPanel = new FlowPanel {
          contents += new Label("Enter your secret password here ")
          val field = new PasswordField(10)
          contents += field
          val label = new Label(field.text)
          contents += label
          listenTo(field)
          reactions += {
            case EditDone(`field`) => label.text = field.password.mkString
          }
        }

        pages += new Page("Password", password, "Password tooltip")
        pages += new Page("Painting", LinePainting.ui)
        //pages += new Page("Text Editor", TextEditor.ui)
      }

      val list: ListView[TabbedPane.Page] = new ListView(tabs.pages) {
        selectIndices(0)
        selection.intervalMode = ListView.IntervalMode.Single
        renderer = ListView.Renderer(_.title)
      }
      val center: SplitPane = new SplitPane(Orientation.Vertical, new ScrollPane(list), tabs) {
        oneTouchExpandable = true
        continuousLayout = true
      }
      layout(center) = Center

      /*
       * This slider is used above, so we need lazy initialization semantics.
       * Objects or lazy vals are the way to go, but objects give us better
       * type inference at times.
       */
      object slider extends Slider {
        min   = 0
        value = tabs.selection.index
        max   = tabs.pages.size - 1
        majorTickSpacing = 1
      }

      layout(slider) = South

      /*
       * Establish connection between the tab pane, slider, and list view.
       */
      listenTo(slider)
      listenTo(tabs.selection)
      listenTo(list.selection)
      reactions += {
        case ValueChanged(`slider`) =>
          if (!slider.adjusting || reactLive) tabs.selection.index = slider.value
        case SelectionChanged(`tabs`) =>
          slider.value = tabs.selection.index
          list.selectIndices(tabs.selection.index)
        case SelectionChanged(`list`) =>
          if (list.selection.items.length == 1)
            tabs.selection.page = list.selection.items(0)
      }
    }
  }
}
