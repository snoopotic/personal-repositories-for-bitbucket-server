package org.networkedassets.atlassian.stash.privaterepos.repositories.ao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.java.ao.Query;

import org.networkedassets.atlassian.stash.privaterepos.repositories.Owner;
import org.networkedassets.atlassian.stash.privaterepos.repositories.PersonalRepositoriesService;
import org.networkedassets.atlassian.stash.privaterepos.repositories.PersonalRepository;
import org.networkedassets.atlassian.stash.privaterepos.repositories.SortCriteria;
import org.networkedassets.atlassian.stash.privaterepos.repositories.SortField;
import org.networkedassets.atlassian.stash.privaterepos.repositories.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.tx.Transactional;
import com.atlassian.stash.project.Project;
import com.atlassian.stash.project.ProjectService;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.repository.RepositoryService;
import com.atlassian.stash.user.StashUser;
import com.atlassian.stash.user.UserService;
import com.atlassian.stash.util.Page;
import com.atlassian.stash.util.PageImpl;
import com.atlassian.stash.util.PageRequest;

@Transactional
@Component
public class AoPersonalRepositoriesService implements
		PersonalRepositoriesService {

	@Autowired
	private ActiveObjects ao;
	@Autowired
	private UserService userService;
	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	private ProjectService projectService;

	private Logger log = LoggerFactory
			.getLogger(AoPersonalRepositoriesService.class);

	@Override
	public Page<Owner> getPersonalRepositoriesOwners(PageRequest pageRequest,
			SortCriteria sort) {
		if (sort.getField().equals(SortField.SIZE)) {
			return getPersonalRepositoriesOwnersSortedBySize(pageRequest,
					sort.getDirection());
		} else {
			return getPersonalRepositoriesOwnersSortedByName(pageRequest,
					sort.getDirection());
		}
	}

	private Page<Owner> getPersonalRepositoriesOwnersSortedByName(
			PageRequest pageRequest, SortOrder direction) {

		int ownersCount = ao.count(Owner.class);

		Owner[] allOwners = ao.find(Owner.class);
		Map<Integer, Owner> ownersByUserIdMap = createOwnersByUserIdMap(allOwners);

		Set<? extends StashUser> stashUsers = userService
				.getUsersById(getStashUserIds(allOwners));

		List<StashUser> sortedStashUsers = sortStashUsersByName(stashUsers,
				direction);
		List<StashUser> sortedStashUsersPage = sortedStashUsers.subList(
				pageRequest.getStart(),
				Math.min(pageRequest.getStart() + pageRequest.getLimit(), ownersCount));

		List<Owner> sortedOwnersPage = new ArrayList<Owner>();

		for (StashUser stashUser : sortedStashUsersPage) {
			sortedOwnersPage.add(ownersByUserIdMap.get(stashUser.getId()));
		}
		return new PageImpl<Owner>(pageRequest, sortedOwnersPage, isLastPage(
				pageRequest, ownersCount));
	}

	private Map<Integer, Owner> createOwnersByUserIdMap(Owner[] owners) {
		Map<Integer, Owner> ownersMap = new HashMap<Integer, Owner>();
		for (Owner owner : owners) {
			ownersMap.put(owner.getUserId(), owner);
		}
		return ownersMap;
	}

	private List<StashUser> sortStashUsersByName(
			Set<? extends StashUser> stashUsers, SortOrder direction) {
		List<StashUser> usersList = new ArrayList<StashUser>(stashUsers);
		Comparator<StashUser> comparator = new Comparator<StashUser>() {
			@Override
			public int compare(StashUser o1, StashUser o2) {
				return o1.getName().compareTo(o2.getName());
			}
		};
		if (direction == SortOrder.DESC) {
			comparator = Collections.reverseOrder(comparator);
		}
		Collections.sort(usersList, comparator);
		return usersList;
	}

	private Set<Integer> getStashUserIds(Owner[] owners) {
		Set<Integer> ids = new HashSet<Integer>();
		for (Owner owner : owners) {
			ids.add(owner.getUserId());
		}
		return ids;
	}

	private Page<Owner> getPersonalRepositoriesOwnersSortedBySize(
			PageRequest pageRequest, SortOrder direction) {
		int ownersCount = ao.count(Owner.class);

		Owner[] owners = ao.find(
				Owner.class,
				Query.select()
						.order("REPOSITORIES_SIZE " + direction.toString())
						.offset(pageRequest.getStart())
						.limit(pageRequest.getLimit()));

		return new PageImpl<Owner>(pageRequest, Arrays.asList(owners),
				isLastPage(pageRequest, ownersCount));
	}

	private boolean isLastPage(PageRequest pageRequest, Integer totalCount) {
		return pageRequest.getStart() + pageRequest.getLimit() <= totalCount;
	}

	private List<PersonalRepository> getUserPersonalRepositories(StashUser user) {
		Owner owner = findOwner(user);
		return Arrays.asList(owner.getRepositories());
	}

	@Override
	public List<PersonalRepository> getUserPersonalRepositories(int userId) {
		StashUser stashUser = userService.getUserById(userId);
		return getUserPersonalRepositories(stashUser);
	}

	@Override
	public List<PersonalRepository> addUserPersonalRepositories(StashUser user,
			Iterable<? extends Repository> repositories) {

		List<PersonalRepository> personalRepos = new ArrayList<PersonalRepository>();

		if (isIterableEmpty(repositories)) {
			return personalRepos;
		}

		log.debug("Adding user {} personal Repos", user.getName());
		Owner owner = findOrCreateOwner(user);
		log.debug("Owner found/created {}", owner);

		for (Repository repo : repositories) {
			personalRepos.add(addPersonalRepository(repo, owner));
		}
		updateOwnerRepositoriesSize(owner);

		return personalRepos;
	}

	private boolean isIterableEmpty(
			@SuppressWarnings("rawtypes") Iterable iterable) {
		return !iterable.iterator().hasNext();
	}

	private Owner findOrCreateOwner(StashUser user) {
		Owner owner = findOwner(user);

		if (owner == null) {
			owner = createOwner(user);
		}

		return owner;
	}

	private Owner findOwner(StashUser user) {
		log.debug("Searching Personal Repository Owner by user", user);
		Owner[] owners = ao.find(Owner.class,
				Query.select().where("USER_ID = ?", user.getId()));
		if (owners.length == 0) {
			return null;
		} else if (owners.length > 1) {
			throw new IllegalStateException(
					"There should never be two Owner entities with the same User Id");
		} else {
			return owners[0];
		}
	}

	private Owner createOwner(StashUser user) {
		log.debug("Creating repository owner from {}", user.getName());
		Owner owner = ao.create(Owner.class);
		owner.setRepositoriesSize(Long.valueOf(0));
		owner.setUserId(user.getId());
		owner.save();
		return owner;
	}

	private PersonalRepository addPersonalRepository(Repository repo,
			Owner owner) {

		PersonalRepository personalRepository = ao
				.create(PersonalRepository.class);
		personalRepository.setRepositoryId(repo.getId());
		personalRepository.setOwner(owner);
		long repositorySize = calculateRepositorySize(repo);
		personalRepository.setRepositorySize(repositorySize);
		personalRepository.save();

		return personalRepository;
	}

	private long calculateRepositorySize(Repository repo) {
		return repositoryService.getSize(repo);
	}

	@Override
	public PersonalRepository addPersonalRepository(Repository repository) {
		StashUser user = getRepositoryOwner(repository);
		Owner owner = findOrCreateOwner(user);

		PersonalRepository personalRepo = addPersonalRepository(repository,
				owner);
		updateOwnerRepositoriesSize(owner);
		return personalRepo;
	}

	private void updateOwnerRepositoriesSize(Owner owner) {
		PersonalRepository[] repositories = owner.getRepositories();
		long totalSize = 0;
		for (PersonalRepository repo : repositories) {
			totalSize += repo.getRepositorySize();
		}

		owner.setRepositoriesSize(totalSize);
		owner.save();
	}

	private StashUser getRepositoryOwner(Repository repository) {
		Project project = repository.getProject();
		return findUserFromProject(project);
	}

	private StashUser findUserFromProject(Project project) {
		String userSlug = findUserSlugFromProjectKey(project.getKey());
		return userService.getUserBySlug(userSlug);
	}

	/**
	 * Cut the ~ from the beginning
	 */
	private String findUserSlugFromProjectKey(String key) {
		return key.substring(1);
	}

	@Override
	public void deletePersonalRepository(Repository repo) {
		PersonalRepository personalRepo = findPersonalRepository(repo);
		Owner repoOwner = personalRepo.getOwner();
		ao.delete(personalRepo);
		updateOwnerRepositoriesSize(repoOwner);
	}

	private PersonalRepository findPersonalRepository(Repository repo) {
		PersonalRepository[] personalRepos = ao.find(PersonalRepository.class,
				Query.select().where("REPOSITORY_ID = ?", repo.getId()));
		if (personalRepos.length == 0) {
			return null;
		} else if (personalRepos.length > 1) {
			throw new IllegalStateException(
					"There should never be two Personal Repository entities with the same Repository Id");
		} else {
			return personalRepos[0];
		}
	}

	@Override
	public int getOwnersCount() {
		return ao.count(Owner.class);
	}

}
