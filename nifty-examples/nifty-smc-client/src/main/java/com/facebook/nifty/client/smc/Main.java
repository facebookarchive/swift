package com.facebook.nifty.client.smc;

import com.facebook.services.ServiceException;
import com.facebook.services.ServiceManager;
import com.facebook.services.Tier;
import org.apache.thrift.TException;
import org.iq80.cli.Arguments;
import org.iq80.cli.Cli;
import org.iq80.cli.Command;
import org.iq80.cli.Help;
import org.iq80.cli.Option;

import java.util.List;

/**
 * Run <code>ssh -f your.dev.server -L 9099:localhost:9099 -N</code> from your laptop.
 */
public class Main {
  public static void main(String[] args) {
    Cli.CliBuilder<Runnable> builder = Cli.buildCli("smcc", Runnable.class)
      .withDescription("smcc")
      .withDefaultCommand(Help.class)
      .withCommands(
        Help.class,
        Print.class,
        ListChildren.class
      );

    Cli<Runnable> cliParser = builder.build();
    cliParser.parse(args).run();
  }

  @Command(
    name = "print",
    description = "Dumps the configuration information for this tier in JSON"
  )
  public static class Print extends BaseSmcCommand {

    @Arguments(required = true)
    public String tier;

    @Override
    public void run() {
      withSmcClient(new SmcClientCallback() {
        @Override
        public void withSmcClient(ServiceManager.Client client) throws TException, ServiceException {
          Tier t = client.getTierByName(tier);
          System.out.println(toPrettyJson(t));
        }
      });
    }
  }

  @Command(
    name = "list-children",
    description = "Lists child tiers of tier(s), recursively if -r is specified."
  )
  public static class ListChildren extends BaseSmcCommand {

    @Option(
      name = "-r",
      description = "recursive"
    )
    public boolean recursive = false;

    @Arguments(required = true)
    public List<String> tiers;

    @Override
    public void run() {
      withSmcClient(new SmcClientCallback() {
        @Override
        public void withSmcClient(ServiceManager.Client client) throws TException, ServiceException {
          for (String tier : tiers) {
            List<String> list = client.getTierNamesByParentTierName(tier, recursive);
            for (String child : list) {
              System.out.println(child);
            }
          }
        }
      });
    }
  }
}

