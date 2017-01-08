/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package richtercloud.reflection.form.builder.jpa.panels;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.swing.JOptionPane;
import org.apache.commons.io.FileUtils;
import richtercloud.message.handler.Message;
import richtercloud.message.handler.MessageHandler;

/**
 *
 * @author richter
 */
public abstract class AbstractFileQueryHistoryEntryStorage implements QueryHistoryEntryStorage {
    private final File file;
    private final Map<Class<?>, List<QueryHistoryEntry>> cache;
    /**
     * The maximum of entries per class.
     */
    private int entryMax = 20;
    private final MessageHandler messageHandler;
    /**
     * Accepts pointers to {@code cache} which trigger the synchronization to
     * {@code file} asynchronously in {@code fileStoreThread}. Sending
     * {@code null} indicates that the thread ought to shut down.
     */
    private final BlockingQueue<Map<Class<?>, List<QueryHistoryEntry>>> fileStoreThreadQueue = new LinkedBlockingQueue<>();
    private final Thread fileStoreThread = new Thread() {
        @Override
        public void run() {
            try {
                Map<Class<?>, List<QueryHistoryEntry>> head = fileStoreThreadQueue.take();
                while(head != null) {
                    try {
                        store(head);
                    } catch (IOException ex) {
                        messageHandler.handle(new Message(ex, JOptionPane.ERROR_MESSAGE));
                    }
                    head = fileStoreThreadQueue.take();
                }
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
    };

    public AbstractFileQueryHistoryEntryStorage(File file,
            MessageHandler messageHandler) throws ClassNotFoundException, IOException {
        this.file = file;
        this.messageHandler = messageHandler;
        if(!file.exists()) {
            FileUtils.touch(file);
            this.cache = new HashMap<>();
        }else {
            Map<Class<?>, List<QueryHistoryEntry>> cache0 = init();
            if(cache0 != null) {
                cache = cache0;
            }else {
                cache = new HashMap<>();
            }
        }
        fileStoreThread.start();
    }

    @Override
    protected void finalize() throws Throwable {
        fileStoreThreadQueue.offer(null);
        fileStoreThread.join();
        super.finalize();
    }

    protected abstract void store(Map<Class<?>, List<QueryHistoryEntry>> head) throws IOException;

    protected abstract Map<Class<?>, List<QueryHistoryEntry>> init() throws IOException, ClassNotFoundException;

    public File getFile() {
        return file;
    }

    @Override
    public void store(Class<?> clazz, QueryHistoryEntry entry) throws QueryHistoryEntryStorageException {
        List<QueryHistoryEntry> entries = cache.get(clazz);
        if(entries == null) {
            entries = new LinkedList<>();
            cache.put(clazz,
                    entries);
        }
        if(entries.size() > entryMax) {
            Iterator<QueryHistoryEntry> entriesItr = entries.iterator();
            QueryHistoryEntry leastUsed = entriesItr.next();
            while(entriesItr.hasNext()) {
                QueryHistoryEntry entryNxt = entriesItr.next();
                if(entryNxt.getUsageCount() < leastUsed.getUsageCount()) {
                    leastUsed = entryNxt;
                }
            }
            entries.remove(leastUsed);
        }
        if(entries.contains(entry)) {
            entries.remove(entry);
                //otherwise usageCount and lastUsage aren't updated
        }
        entries.add(entry);
        fileStoreThreadQueue.offer(cache);
    }

    @Override
    public List<QueryHistoryEntry> retrieve(Class<?> clazz) {
        List<QueryHistoryEntry> retValue = cache.get(clazz);
        if(retValue == null) {
            retValue = new LinkedList<>();
        }
        return retValue;
    }

    @Override
    public QueryHistoryEntry getInitialEntry(Class<?> clazz) {
        List<QueryHistoryEntry> entries = cache.get(clazz);
        if(entries == null) {
            return null;
        }
        Iterator<QueryHistoryEntry> entriesItr = entries.iterator();
        QueryHistoryEntry mostUsed = entriesItr.next();
        while(entriesItr.hasNext()) {
            QueryHistoryEntry entriesNxt = entriesItr.next();
            if(entriesNxt.getUsageCount() > mostUsed.getUsageCount()) {
                mostUsed = entriesNxt;
            }
        }
        return mostUsed;
    }
}
