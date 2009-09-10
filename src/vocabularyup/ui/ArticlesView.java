package vocabularyup.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import vocabularyup.VocabularyApp;
import vocabularyup.VocabularyAppAdapter;
import vocabularyup.VocabularyAppEvent;
import vocabularyup.model.xml.Article;

/**
 * Contains list of articles(result of search)
 * @author dooman
 */
public class ArticlesView extends JTable {
    private static final Logger log = Logger.getLogger(ArticlesView.class.getName());

    public static class ArticleView {
        private Article article;

        public ArticleView(Article article) {
            this.article = article;
        }

        public Article getArticle() {
            return article;
        }

        @Override
        public String toString() {
            return article.getSource();
        }
    }

    private class ArticlesViewModel extends AbstractTableModel {
        private List<ArticleView> searchResults = new ArrayList<ArticleView>();

        public ArticlesViewModel() {
            VocabularyApp.getInstance().addListener(new VocabularyAppAdapter(){
                @Override
                public void currentArticlesChanges(VocabularyAppEvent event) {
                    VocabularyApp.getInstance().setSelectedArticle(null);
                    setSearchResults(event.getArticles());
                }
            });
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public int getRowCount() {
            return searchResults.size();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return searchResults.get(rowIndex);
        }

        public void setSearchResults(List<Article> newSearchResults) {
            searchResults.clear();
            for (Article a : newSearchResults) {
                addSearchResult(a);
            }
            fireTableDataChanged();
            if (newSearchResults.size() > 0) {
                log.fine("Set select in articles view to [0]");
                changeSelection(0, 0, false, false);
            }
        }

        public void addSearchResult(Article searchResult) {
            searchResults.add(new ArticleView(searchResult));
        }

        public void clearResult() {
            searchResults.clear();
        }
    }

    private ArticlesViewModel model = new ArticlesViewModel();

    public ArticlesView() {
        setModel(model);
    }
}
